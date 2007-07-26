package ru.org.linux.site;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.sql.*;
import java.util.logging.Logger;

import ru.org.linux.util.*;

public class Comment implements Serializable {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private final int msgid;
  private final String title;
  private final String photo;
  private final String nick;
  private final int userScore;
  private final int userMaxScore;
  private final int replyto;
  private final int topic;
  private final boolean deleted;
  private final Timestamp postdate;
  private final String message;

  public Comment(ResultSet rs) throws SQLException {
    msgid=rs.getInt("msgid");
    title=StringUtil.makeTitle(rs.getString("title"));
    photo=rs.getString("photo");
    nick=rs.getString("nick");
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    postdate=rs.getTimestamp("postdate");
    userScore=rs.getInt("score");
    userMaxScore=rs.getInt("max_score");
    message=rs.getString("message");
  }

  public Comment(Connection db, int msgid) throws SQLException, MessageNotFoundException {
    Statement st = db.createStatement();

    ResultSet rs=st.executeQuery("SELECT " +
        "postdate, topic, nick, score, max_score, comments.id as msgid, comments.title, " +
        "photo, deleted, replyto, message " +
        "FROM comments, users, msgbase " +
        "WHERE comments.id="+msgid+" AND comments.id=msgbase.id AND comments.userid=users.id");

    if (!rs.next()) throw new MessageNotFoundException(msgid);

    this.msgid=rs.getInt("msgid");
    title=StringUtil.makeTitle(rs.getString("title"));
    photo=rs.getString("photo");
    nick=rs.getString("nick");
    topic=rs.getInt("topic");
    replyto=rs.getInt("replyto");
    deleted=rs.getBoolean("deleted");
    postdate=rs.getTimestamp("postdate");
    userScore=rs.getInt("score");
    userMaxScore=rs.getInt("max_score");
    message=rs.getString("message");

    st.close();
  }

  public String printMessage(Template tmpl, Connection db, boolean showMenu, String urladd, boolean moderatorMode, String user, boolean expired)
      throws IOException, SQLException, UtilException {
    StringBuffer out=new StringBuffer();

    out.append("\n\n<!-- ").append(msgid).append(" -->\n");

    out.append("<table width=\"100%\" cellspacing=0 cellpadding=0 border=0>");

    if (showMenu) {
      out.append("<tr class=title><td>");

      if (!deleted) {
        out.append("[<a href=\"/jump-message.jsp?msgid=").append(topic).append('#').append(msgid).append("\">#</a>]");
      }

      if (!expired && !deleted) {
        out.append("[<a href=\"add_comment.jsp?topic=").append(topic).append("&amp;replyto=").append(msgid).append(urladd).append("\">��������</a>]");
      }

      if (!deleted && (moderatorMode || nick.equals(user))) {
        out.append("[<a href=\"delete_comment.jsp?msgid=").append(msgid).append("\">�������</a>]");
      }

      if (moderatorMode) {
        out.append("[<a href=\"sameip.jsp?msgid=").append(msgid).append("\">������ � ����� IP</a>]");
      }

      if (deleted) {
        DeleteInfo deleteInfo = DeleteInfo.getDeleteInfo(db, msgid);

        if (deleteInfo==null) {
          out.append("<strong>��������� �������</strong>");
        } else {
          out.append("<strong>��������� ������� ").append(deleteInfo.getNick()).append(" �� ������� '").append(HTMLFormatter.htmlSpecialChars(deleteInfo.getReason())).append("'</strong>");
        }
      }

      out.append("&nbsp;</td></tr>");
    }

    if (replyto!=0 && showMenu) {
      out.append("<tr class=title><td>");
      Statement rts=db.createStatement();
      ResultSet rt=rts.executeQuery("SELECT users.nick, comments.title, comments.postdate FROM comments, users WHERE users.id=comments.userid AND comments.id=" + replyto);

      if (rt.next())
        out.append("����� ��: <a href=\"").append("view-message.jsp?msgid=").append(topic).append('#').append(replyto).append("\">").append(StringUtil.makeTitle(rt.getString("title"))).append("</a> �� ").append(rt.getString("nick")).append(' ').append(Template.dateFormat.format(rt.getTimestamp("postdate")));

      rt.close();
      rts.close();
    }

    out.append("<tr class=body><td>");
    out.append("<div class=msg>");

    boolean tbl = false;
    if (photo!=null) {
      if (tmpl.getProf().getBoolean("photos")) {
        out.append("<table><tr><td valign=top align=center>");
        tbl=true;

        try {
          ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+"/photos/"+photo);
          out.append("<img src=\"/photos/").append(photo).append("\" alt=\"").append(nick).append(" (����������)\" ").append(info.getCode()).append(" >");
        } catch (BadImageException e) {
          logger.warning(StringUtil.getStackTrace(e));
        }

        out.append("</td><td valign=top>");
      }
    }

    out.append("<h2><a name=").append(msgid).append('>').append(title).append("</a></h2>");

//    out.append(storage.readMessage("msgbase", String.valueOf(msgid)));
    out.append(message);

    out.append("<p><i>").append(nick).append(' ');

    if (!"anonymous".equals(nick)) {
      out.append(User.getStars(userScore, userMaxScore)).append(' ');
        if (moderatorMode)
          out.append("(Score: ").append(userScore).append(" MaxScore: ").append(userMaxScore).append(") ");
    }

    out.append("(<a href=\"whois.jsp?nick=").append(URLEncoder.encode(nick)).append("\">*</a>) (").append(Template.dateFormat.format(postdate)).append(")</i>");

    if (!expired && !deleted && showMenu)
      out.append("<p><font size=2>[<a href=\"add_comment.jsp?topic=").append(topic).append("&amp;replyto=").append(msgid).append(urladd).append("\">�������� �� ��� ���������</a>]</font>");

    if (tbl) out.append("</td></tr></table>");
      out.append("</div></td></tr>");
      out.append("</table><p>");

    return out.toString();
  }

  public int getMessageId() {
    return msgid;
  }

  public int getReplyTo() {
    return replyto;
  }

  public boolean isAnonymous() {
    return "anonymous".equals(nick) || userScore<User.ANONYMOUS_LEVEL_SCORE;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public int getTopic() {
    return topic;
  }

  public String getTitle() {
    return title;
  }
}
