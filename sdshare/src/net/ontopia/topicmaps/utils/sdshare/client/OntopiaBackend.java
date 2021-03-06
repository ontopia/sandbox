
package net.ontopia.topicmaps.utils.sdshare.client;

import java.net.URL;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ontopia.utils.StringUtils;
import net.ontopia.utils.CompactHashSet;
import net.ontopia.utils.OntopiaRuntimeException;
import net.ontopia.xml.DefaultXMLReaderFactory;
import net.ontopia.infoset.core.LocatorIF;
import net.ontopia.infoset.impl.basic.URILocator;
import net.ontopia.topicmaps.core.*;
import net.ontopia.topicmaps.xml.XTMTopicMapReader;
import net.ontopia.topicmaps.entry.TopicMaps;
import net.ontopia.topicmaps.entry.TopicMapReferenceIF;
import net.ontopia.topicmaps.entry.TopicMapRepositoryIF;
import net.ontopia.topicmaps.utils.MergeUtils;
import net.ontopia.topicmaps.utils.KeyGenerator;

public class OntopiaBackend extends AbstractBackend implements ClientBackendIF {
  static Logger log = LoggerFactory.getLogger(OntopiaBackend.class.getName());

  private TopicMapRepositoryIF repository; // injected from outside
  
  public void loadSnapshot(Snapshot snapshot, URIResolverIF resolver) {
    TopicMapStoreIF store = null;
    try {
      String url = findPreferredLink(snapshot.getLinks()).getUri();
      LocatorIF base = URILocator.create(snapshot.getFeed().getPrefix());
      
      XTMTopicMapReader reader = new XTMTopicMapReader(new URL(url).openConnection().getInputStream(), base);
      store = getStore(endpoint.getHandle());
      TopicMapIF tm = store.getTopicMap();
      try {
        reader.importInto(tm);
        store.commit();
      } finally {
        store.abort();
      }
    } catch (IOException e) {
      throw new OntopiaRuntimeException(e);
    } finally {
      if (store != null)
        store.close();
    }
  }

  public void applyFragments(List<Fragment> fragments, URIResolverIF resolver) {
    if (fragments.isEmpty())
      return; // avoids IndexOutOfBoundsException later
    
    boolean committed = false;
    TopicMapStoreIF store = null;
    try {
      // (2) start a transaction      
      store = getStore(endpoint.getHandle());
      
      // (3) applying the fragment
      try {
        String prefix = fragments.get(0).getFeed().getPrefix();
        TopicMapIF topicmap = store.getTopicMap();
        for (Fragment fragment : fragments)
          applyFragment(prefix, fragment, topicmap, resolver);
      } catch (Throwable e) {
        store.abort();
        throw new OntopiaRuntimeException(e);
      }
      
      // (4) commit
      store.commit();
    } finally {
      if (store != null)
        store.close(); // recycle the store
    }
  }

  public void setRepository(TopicMapRepositoryIF repository) {
    this.repository = repository;
  }

  // ===== INTERNAL IMPLEMENTATION CODE

  public int getLinkScore(AtomLink link) {
    return Utils.getLinkScore(link);
  }

  // overriden by test code to sneak in test TM
  protected TopicMapStoreIF getStore(String id) {
    if (repository != null)
      return repository.createStore(id, false);

    // fallback alternative if we don't have a repository
    return TopicMaps.createStore(id, false);
  }
  
  private void applyFragment(String prefix, Fragment fragment,
                             TopicMapIF topicmap, URIResolverIF resolver)
    throws IOException {
    AtomLink link = findPreferredLink(fragment.getLinks());    
    String url = null;
    if (link != null)
      url = link.getUri();

    if (fragment.getTopicSIs().isEmpty() &&
        fragment.getTopicIIs().isEmpty() &&
        fragment.getTopicSLs().isEmpty())
      throw new OntopiaRuntimeException("Fragment " + url + " had no identity");
    
    // (1) get the fragment
    // FIXME: for now we only support XTM
    LocatorIF base;
    if (url != null)
      base = URILocator.create(url);
    else
      base = URILocator.create("http://bogus.workaround.uri"); // for push
    Reader input = resolve(fragment, url, resolver);
    XTMTopicMapReader reader = new XTMTopicMapReader(input, base);
    reader.setFollowTopicRefs(false);
    TopicMapIF tmfragment = reader.read();
    
    // (2) apply it
    TopicIF ftopic = getTopic(tmfragment, fragment);
    TopicIF ltopic;
    if (ftopic != null)
      ltopic = MergeUtils.findTopic(topicmap, ftopic);
    else
      ltopic = getTopic(topicmap, fragment);

    // (a) check if we need to create the topic
    if (ltopic == null && ftopic != null)
      // the topic exists in the source, but not in the target, therefore
      // we create it
      ltopic = topicmap.getBuilder().makeTopic();

    // it's possible that the topic is deleted in the source, and also
    // already deleted in the target (e.g because the fragment has
    // already been processed). in that case we don't need to do
    // anything.
    if (ltopic == null && ftopic == null)
      return;
    
    // there might not be a fragment topic, which indicates that it's been
    // deleted. in this case we make a blank dummy that will cause everything
    // to be deleted, and the local topic to be deleted at the end.
    if (ftopic == null)
      ftopic = tmfragment.getBuilder().makeTopic();

    // (b) copy across all identifiers
    ltopic = MergeUtils.copyIdentifiers(ltopic, ftopic);

    // (c) merge all topics in the fragment into the target. this is
    // necessary so that all types, scoping topics, and associated
    // topics are actually present in the target when we update the
    // topic characteristics. (however, we can't merge in the current
    // topic, as the procedure for that one is a bit more complex.)
    for (TopicIF oftopic : tmfragment.getTopics())
      if (oftopic != ftopic)
        MergeUtils.mergeInto(topicmap, oftopic);

    // (d) sync the types
    syncTypes(ftopic, ltopic);

    // (e) sync the names
    Map<String, ? extends ReifiableIF> keymap = makeKeyMap(ftopic.getTopicNames(), topicmap);
    syncCollection(ftopic, ltopic, keymap, ltopic.getTopicNames(), prefix);

    // (f) sync the occurrences
    keymap = makeKeyMap(ftopic.getOccurrences(), topicmap);
    syncCollection(ftopic, ltopic, keymap, ltopic.getOccurrences(), prefix);
    
    // (g) sync the associations
    keymap = makeKeyMap(getAssociations(ftopic), topicmap);
    syncCollection(ftopic, ltopic, keymap, getAssociations(ltopic), prefix);

    // (h) is the topic deleted
    if (ltopic.getTopicNames().isEmpty() &&
        ltopic.getOccurrences().isEmpty() &&
        ltopic.getRoles().isEmpty())
      ltopic.remove(); // empty topic, therefore remove
  }
  
  // goal: after returning, ltopic is to have the same set of types as
  // ftopic, after making a minimal number of changes
  private void syncTypes(TopicIF ftopic, TopicIF ltopic) {
    // translate the set of types
    Collection<TopicIF> ftypes = translate(ltopic.getTopicMap(),
                                           ftopic.getTypes());
    
    // first, remove types ltopic has that ftopic doesn't
    for (TopicIF ltype : new ArrayList<TopicIF>(ltopic.getTypes()))
      if (!ftypes.contains(ltype))
        ltopic.removeType(ltype);

    // second, add types ftopic has that ltopic doesn't
    Collection<TopicIF> ltypes = ltopic.getTypes();
    for (TopicIF ftype : ftypes)
      if (!ltypes.contains(ftype))
        ltopic.addType(ftype);
  }

  // returns the corresponding set of topics in the local topic map
  private Collection<TopicIF> translate(TopicMapIF ltm,
                                        Collection<TopicIF> ftopics) {
    Collection<TopicIF> ltopics = new CompactHashSet(ftopics.size());
    for (TopicIF ftopic : ftopics) {
      TopicIF ltopic = MergeUtils.findTopic(ltm, ftopic);
      if (ltopic == null)
        ltopic = MergeUtils.mergeInto(ltm, ftopic);
      ltopics.add(ltopic);
    }
    return ltopics;
  }

  private Map<String, ? extends ReifiableIF>
  makeKeyMap(Collection<? extends ReifiableIF> objects, TopicMapIF othertm) {
    Map<String, ReifiableIF> keymap = new HashMap();
    for (ReifiableIF object : objects)
      keymap.put(KeyGenerator.makeKey(object, othertm), object);
    return keymap;
  }

  private void syncCollection(TopicIF ftopic, TopicIF ltopic,
                              Map<String,? extends ReifiableIF> keymap,
                              Collection<? extends ReifiableIF> lobjects,
                              String prefix) {
    // check all local objects against the fragment
    lobjects = new ArrayList<ReifiableIF>(lobjects); // avoid concmodexc
    for (ReifiableIF lobject : lobjects) {
      String key = KeyGenerator.makeKey(lobject);
      ReifiableIF fobject = keymap.get(key);

      if (fobject == null) {
        // the source doesn't have this object. however, if it still has
        // item identifiers (other than ours), we can keep it. we do
        // need to remove any iids starting with the prefix, though.
        pruneItemIdentifiers(lobject, prefix);
        if (lobject.getItemIdentifiers().isEmpty())
          lobject.remove();
      } else {
        // the source has this object. we need to make sure the local
        // copy has the item identifier.
        addItemIdentifier(lobject, prefix);
        keymap.remove(key); // we've seen this one, so cross it off
      }
    }

    // copy the objects in the source which are not in the local copy
    // across to the local copy, adding item identifiers
    for (ReifiableIF fobject : keymap.values()) {
      ReifiableIF lobject = MergeUtils.mergeInto(ltopic, fobject);
      addItemIdentifier(lobject, prefix);
    }
  }

  private void pruneItemIdentifiers(TMObjectIF object, String prefix) {
    for (LocatorIF iid : new ArrayList<LocatorIF>(object.getItemIdentifiers()))
      if (iid.getAddress().startsWith(prefix))
        object.removeItemIdentifier(iid);
  }

  // FIXME: this doesn't really add; it just ensures that the object has
  // one. ie: method is idempotent.
  private void addItemIdentifier(TMObjectIF object, String prefix) {
    for (LocatorIF iid : object.getItemIdentifiers())
      if (iid.getAddress().startsWith(prefix))
        return; // it already has one; we're done

    // it doesn't really matter what the iid is, so long as it is
    // unique and starts with the right prefix. we therefore take what
    // we assume is the shortest path to the goal.
    TopicMapIF topicmap = object.getTopicMap();
    LocatorIF base = URILocator.create(prefix);
    String objectid = object.getObjectId();
    LocatorIF iid = base.resolveAbsolute("#sd" + objectid);
    while (topicmap.getObjectByItemIdentifier(iid) != null)
      iid = base.resolveAbsolute("#sd" + objectid + '-' +
                                 StringUtils.makeRandomId(5));

    object.addItemIdentifier(iid);
  }

  private Collection<ReifiableIF> getAssociations(TopicIF topic) {
    Collection<ReifiableIF> assocs = new ArrayList<ReifiableIF>();
    for (AssociationRoleIF role : topic.getRoles())
      assocs.add(role.getAssociation());
    return assocs;
  }  

  private Reader resolve(Fragment fragment, String url, URIResolverIF resolver)
    throws IOException {
    if (url != null)
      return resolver.downloadData(url);
    else if (fragment.getContent() != null)
      return new StringReader(fragment.getContent());
    else
      throw new OntopiaRuntimeException("Fragment contained neither " +
                                        "acceptable links nor content");
  }

  private TopicIF getTopic(TopicMapIF topicmap, Fragment f) {
    TopicIF topic = null;
    
    for (String si : f.getTopicSIs()) {
      LocatorIF psi = URILocator.create(si);
      TopicIF t = topicmap.getTopicBySubjectIdentifier(psi);
      if (topic == null)
        topic = t;
      else if (t != null && t != topic)
        MergeUtils.mergeInto(topic, t); // merging the topics first
    }

    for (String sl : f.getTopicSLs()) {
      LocatorIF psi = URILocator.create(sl);
      TopicIF t = topicmap.getTopicBySubjectLocator(psi);
      if (topic == null)
        topic = t;
      else if (t != null && t != topic)
        MergeUtils.mergeInto(topic, t); // merging the topics first
    }

    for (String ii : f.getTopicIIs()) {
      LocatorIF psi = URILocator.create(ii);
      TopicIF t = (TopicIF) topicmap.getObjectByItemIdentifier(psi);
      if (topic == null)
        topic = t;
      else if (t != null && t != topic)
        MergeUtils.mergeInto(topic, t); // merging the topics first
    }
    
    return topic;
  }
}