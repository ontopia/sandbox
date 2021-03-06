<%@ page 
  language="java" 
  contentType="application/atom+xml; charset=utf-8"
  import="net.ontopia.topicmaps.utils.sdshare.*,
          net.ontopia.topicmaps.entry.TopicMapReferenceIF"
%><%
  String tmid = request.getParameter("topicmap");
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
  AtomWriter atom = new AtomWriter(out);
  atom.startFeed("Collection feed for " + ref.getTitle(),
                 System.currentTimeMillis(),
                 StartUpServlet.getEndpointURL() + tmid);

  // FRAGMENTS
  atom.startEntry("Fragments for " + ref.getTitle(), 
                  StartUpServlet.getEndpointURL() + tmid + "/fragments", 
		  System.currentTimeMillis());
  atom.addLink("changes.jsp?topicmap=" + ref.getId(),
               "application/atom+xml",
               "http://www.egovpt.org/sdshare/fragmentsfeed");
  atom.addLink("changes.jsp?topicmap=" + ref.getId(),
               "application/atom+xml",
               "alternate");
  atom.addLink("changes.jsp?topicmap=" + ref.getId());
  atom.endEntry();

  // SNAPSHOTS
  atom.startEntry("Shapshots for " + ref.getTitle(), 
                  StartUpServlet.getEndpointURL() + tmid + "/snapshots", 
                  System.currentTimeMillis());
  atom.addLink("snapshots.jsp?topicmap=" + ref.getId(),
               "application/atom+xml",
               "http://www.egovpt.org/sdshare/snapshotsfeed");
  atom.addLink("snapshots.jsp?topicmap=" + ref.getId(),
               "application/atom+xml",
               "alternate");
  atom.endEntry();

  atom.endFeed();
%>
