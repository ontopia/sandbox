<%@ page 
  language="java" 
  contentType="application/atom+xml; charset=utf-8"
  import="java.util.List,
          java.util.Collection,
          java.net.InetAddress,
	  java.text.ParseException,
          net.ontopia.infoset.core.LocatorIF,
          net.ontopia.topicmaps.utils.sdshare.*,
          net.ontopia.topicmaps.utils.sdshare.client.*,
          net.ontopia.topicmaps.entry.TopicMapReferenceIF,
	  net.ontopia.topicmaps.core.*"
%><%

  // TODO: should support if-modified-since

  String tmid = request.getParameter("topicmap");
  String since = request.getParameter("since");

  TopicMapTracker tracker = TrackerManager.getTracker(tmid);
  if (tracker == null) {
    // means either there's no such TM, or we are not supposed to produce
    // a feed for it.
    response.setStatus(404);
    response.setHeader("Content-type", "text/plain");
    out.write("No such topic map: '" + tmid + "'.");
    return;
  }

  TopicMapReferenceIF ref = tracker.getReference();
  TopicMapStoreIF store = ref.createStore(true);
  TopicMapIF tm = store.getTopicMap();  
  LocatorIF base = store.getBaseAddress();
  String prefix = StartUpServlet.getTopicMapURL(base, tmid);

  SyntaxIF[] syntaxes = StartUpServlet.getSyntaxes();

  AtomWriter atom = new AtomWriter(out);
  atom.startFeed("Fragments feed for " + ref.getTitle(),
                 System.currentTimeMillis(),
                 prefix + "/fragments");

  atom.addServerPrefix(prefix);

  List<ChangedTopic> changes;
  if (since == null)
     changes = tracker.getChangeFeed();
  else {
     long sincetime = 0;
     try {
       sincetime = FeedReaders.parseDateTime(since);       
     } catch (RuntimeException e) {
       // Web3 sends the old format, so we support a configuration option
       // to ignore the parameter being in the wrong format. we rethrow
       // the exception if it wasn't caused by the format, or the config flag
       // isn't set.
       ServletContext ctxt = pageContext.getServletContext();
       String param = ctxt.getInitParameter("accept-bad-since-format");
       boolean ignoresinceerror = (param != null && param.trim().equals("true"));
       if (!(e.getCause() instanceof ParseException && ignoresinceerror))
         throw e; 
     }
     changes = tracker.getChangeFeed(sincetime);
  }
  for (ChangedTopic change : changes) {
    atom.startEntry("Topic with object ID " + change.getObjectId(),
                    prefix + "/" + change.getObjectId() + "/" + change.getTimestamp(),
		    change.getTimestamp());

    for (int ix = 0; ix < syntaxes.length; ix++)
      atom.addLink("fragment.jsp?topicmap=" + tmid + "&topic=" + change.getObjectId() + "&syntax=" + syntaxes[ix].getId(),
                   syntaxes[ix].getMIMEType(),
                   "alternate");

    Collection<LocatorIF> sis;
    Collection<LocatorIF> iis;
    Collection<LocatorIF> sls;
    if (change.isDeleted()) {
      DeletedTopic delete = (DeletedTopic) change;
      sis = delete.getSubjectIdentifiers();
      sls = delete.getSubjectLocators();
      iis = delete.getItemIdentifiers();
    } else {
      TopicIF topic = (TopicIF) tm.getObjectById(change.getObjectId());
      sis = topic.getSubjectIdentifiers();
      sls = topic.getSubjectLocators();
      iis = topic.getItemIdentifiers();
    }

    if (!sis.isEmpty()) {
      for (LocatorIF loc : sis)
        atom.addTopicSI(loc);
    } else if (!sls.isEmpty()) {
      for (LocatorIF loc : sls)
        atom.addTopicSL(loc);
    } else {
      for (LocatorIF loc : iis)
        atom.addTopicII(loc);
    }

    atom.endEntry();
  }

  atom.endFeed();
%>
