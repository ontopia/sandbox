
package net.ontopia.topicmaps.utils.sdshare.client;

import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ontopia.utils.CompactHashSet;
import net.ontopia.utils.OntopiaRuntimeException;
import net.ontopia.infoset.core.LocatorIF;
import net.ontopia.infoset.impl.basic.URILocator;
import net.ontopia.topicmaps.core.TopicIF;
import net.ontopia.topicmaps.core.TopicMapIF;
import net.ontopia.topicmaps.core.TopicMapStoreIF;
import net.ontopia.topicmaps.entry.TopicMaps;
import net.ontopia.topicmaps.entry.TopicMapReferenceIF;
import net.ontopia.topicmaps.entry.TopicMapRepositoryIF;
import net.ontopia.topicmaps.xml.XTMFragmentExporter;
import net.ontopia.topicmaps.xml.XTMTopicMapFragmentWriter;
import net.ontopia.topicmaps.utils.sdshare.ChangedTopic;
import net.ontopia.topicmaps.utils.sdshare.DeletedTopic;
import net.ontopia.topicmaps.utils.sdshare.TrackerManager;
import net.ontopia.topicmaps.utils.sdshare.TopicMapTracker;

/**
 * INTERNAL: Frontend which gets snapshots and changes directly from
 * the Ontopia API.
 */
public class OntopiaFrontend implements ClientFrontendIF {
  static Logger log = LoggerFactory.getLogger(OntopiaFrontend.class.getName());
  protected String handle; // handle (URL|id) of source collection
  protected TopicMapTracker tracker;
  
  public String getHandle() {
    return handle;
  }

  public SnapshotFeed getSnapshotFeed() throws IOException, SAXException {
    throw new UnsupportedOperationException(); // FIXME: implement!
  }

  public Iterator<FragmentFeed> getFragmentFeeds(Timestamp last)
    throws IOException, SAXException {
    long lastChange = 0;
    if (last != null)
      lastChange = last.getTime(); // FIXME: precision loss
    FragmentFeed feed = new FragmentFeed();
    // FIXME: presumably we need a title and all that jazz, too?

    TopicMapRepositoryIF rep = TopicMaps.getRepository();
    TopicMapReferenceIF ref = rep.getReferenceByKey(handle);
    if (ref == null)
      throw new OntopiaRuntimeException("No such topic map: '" + handle + "'");
    TopicMapIF topicmap = ref.createStore(true).getTopicMap();
    feed.setPrefix(topicmap.getStore().getBaseAddress().getExternalForm());

    // must make a copy, since list can change while we're working
    List<ChangedTopic> changes = new ArrayList(tracker.getChangeFeed(lastChange));
    for (ChangedTopic topic : changes) {
      Set<String> sis = new CompactHashSet();
      Set<String> iis = new CompactHashSet();
      Set<String> sls = new CompactHashSet();
      String fragment;
      if (topic instanceof DeletedTopic) {
        for (LocatorIF si : ((DeletedTopic) topic).getSubjectIdentifiers())
          sis.add(si.getExternalForm());
        for (LocatorIF sl : ((DeletedTopic) topic).getSubjectLocators())
          sls.add(sl.getExternalForm());
        for (LocatorIF ii : ((DeletedTopic) topic).getItemIdentifiers())
          iis.add(ii.getExternalForm());
        fragment = makeFragment(null);
      } else {
        TopicIF rtopic = (TopicIF) topicmap.getObjectById(topic.getObjectId());

        if (rtopic != null) {
          // we need to check if this topic has any identifiers at all.
          // because if it doesn't, then we will be in deep trouble
          // later.
          if (rtopic.getItemIdentifiers().isEmpty() &&
              rtopic.getSubjectIdentifiers().isEmpty() &&
              rtopic.getSubjectLocators().isEmpty())
            addIdentifier(ref, topic.getObjectId());
          
          for (LocatorIF si : rtopic.getSubjectIdentifiers())
            sis.add(si.getExternalForm());
          for (LocatorIF sl : rtopic.getSubjectLocators())
            sls.add(sl.getExternalForm());
          for (LocatorIF ii : rtopic.getItemIdentifiers())
            iis.add(ii.getExternalForm());
        } else {
          // the topic has been deleted, but for some unknown reason we don't
          // have a DeletedTopic for it. anyway, we work around it.
          sis.add(makeVirtualReference(topic.getObjectId()));
        }
          
        fragment = makeFragment(rtopic);
      }

      // the "topic:"-link is interpreted by downloadData() below
      AtomLink link = new AtomLink(null, "topic:" + topic.getObjectId());
      Fragment f = new Fragment(Collections.singleton(link),
                                new Timestamp(topic.getTimestamp()),
                                null);
      if (!sis.isEmpty())
        f.setTopicSIs(sis);
      if (!sls.isEmpty())
        f.setTopicSLs(sls);
      if (!iis.isEmpty())
        f.setTopicIIs(iis);
      feed.addFragment(f);
    }

    return Collections.singleton(feed).iterator();
  }

  public void setSyncSource(SyncSource source) {
    this.handle = source.getHandle();
    this.tracker = TrackerManager.registerTracker(handle);
  }

  private String makeFragment(TopicIF topic) {
    StringWriter out = new StringWriter();
    try {
      XTMTopicMapFragmentWriter writer = new XTMTopicMapFragmentWriter(out);
      writer.startTopicMap();
      if (topic != null)
        writer.exportTopic(topic);
      writer.endTopicMap();
    } catch (IOException e) {
      // should be impossible, but rethrowing just in case
      throw new OntopiaRuntimeException(e);
    }
    return out.toString();
  }

  /**
   * Adds a unique identifier to the topic to ensure that we will be
   * able to apply the fragment to the other side.
   */
  private void addIdentifier(TopicMapReferenceIF ref, String objid)
    throws IOException {
    TopicMapStoreIF store = ref.createStore(false);
    try {
      TopicMapIF tm = store.getTopicMap();
      TopicIF topic = (TopicIF) tm.getObjectById(objid);
      String psi = makeVirtualReference(objid);
      topic.addSubjectIdentifier(URILocator.create(psi));
      store.commit();
    } finally {
      store.close();
    }
  }

  /**
   * Creates a unique identifier used for topics which have no other
   * identifier.
   */
  // originally this was the identifier scheme used:
  //   XTMFragmentExporter.makeVirtualReference(topic, handle)
  // for reasons too embarrassing to reproduce here, we changed to oid:xxx
  // so that it will work for NRK/Skole
  private String makeVirtualReference(String objid) {
    return "oid:" + objid;
  }
  
  /**
   * Returns a reader which can be used to get a data representation.
   * Either a snapshot or a fragment.
   */
  public Reader downloadData(String uri) throws IOException {
    // parse uri
    int pos = uri.indexOf(':');
    String id = uri.substring(pos + 1);
    
    // get topic
    TopicMapRepositoryIF rep = TopicMaps.getRepository();
    TopicMapReferenceIF ref = rep.getReferenceByKey(handle);
    TopicMapIF topicmap = ref.createStore(true).getTopicMap();
    TopicIF topic = (TopicIF) topicmap.getObjectById(id);

    // return data
    return new StringReader(makeFragment(topic));
  }  
}