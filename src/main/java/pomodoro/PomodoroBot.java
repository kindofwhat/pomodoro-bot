package pomodoro;

import com.ullink.slack.simpleslackapi.*;

import java.util.*;

/**
 * Created by christian on 19.04.15.
 */
public class PomodoroBot implements SlackBot, SlackMessageListener {
    private final String id;
    private final String userName;
    private final boolean deleted;
    private final SlackSession session;
    private final PomodoroTask task;

    private final String helpMessage =
                    "```" +
                    "* start (length=25)        start, with length in minutes\n" +
                    "* show  (all)              show status, optionally for all users\n" +
                    "* stop                     stop my pomodoro" +
                    "```";


    public PomodoroBot(String id, String userName, boolean deleted, SlackSession mySession) {
        this.id = id;
        this.userName = userName;
        this.deleted = deleted;
        this.session = mySession;
        this.session.addMessageListener(this);
        this.task = new PomodoroTask(this);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task,new Date(), 1000);

    }



    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void onSessionLoad(SlackSession session) {

    }

    @Override
    public void onMessage(SlackMessage message) {
        String text = message.getMessageContent();
        if(text.startsWith("pomodoro: ")) {
            text = text.substring("pomodoro: ".length());
        }
        reactTo(text.split(" ", 2), message);
    }

    private void reactTo(String[] split, SlackMessage message) {

        //only answer my own messages
        if(!getId().equals(message.getSender().getId())) {
            if(split == null || split.length < 1 || "help".equals(split[0])) {
                reply(helpMessage, message);
            }
            String verb = split[0];
            if("start".equals(verb)) {
                Integer time = 25;
                if(split.length > 1) {
                    try {
                        time = Integer.parseInt(split[1]);
                    } catch (NumberFormatException e) {
                        reply("Was not able to interpret " + time + " as time. Will go with default of 25 minutes", message);
                    }
                }
                startPomodoro(message.getSender(), message.getChannel(), time);
            } else if("stop".equals(verb)) {
                task.stopPomodoro(message.getSender(),message.getChannel());
            } else if("show".equals(verb)) {
                task.showStatus(message.getSender(),message.getChannel());
            }
        }


    }

    private void startPomodoro(SlackUser sender, SlackChannel channel, int time) {
        task.startPomodoro(sender, channel, time);
    }

    public void communicateStart(SlackUser user, SlackChannel channel) {
        sendMessage(user.getRealName() + ": your pomodoro has started. You may want to set the status to away", channel);
    }
    public void communicateStop(SlackUser user, SlackChannel channel) {
        sendMessage(user.getRealName() + ": your pomodoro is up. Relax some", channel);
    }

    public void sendMessage(String message, SlackChannel channel) {
        session.sendMessage(channel,message,null);
    }

    /*
    public void sendMessageToUser(String message, SlackUser user) {
         Optional<SlackChannel> directChannel = session.getChannels().stream().filter(slackChannel -> slackChannel.isDirect() && slackChannel.getMembers().contains(user)).findFirst();
        if(directChannel.isPresent()) {
            session.sendMessage(directChannel.get(),message,null,null,null);
        } else {
            //TODO: logging
        }
    }

    */
    private void reply(String reply, SlackMessage message) {
        session.sendMessage(message.getChannel(), reply,null);
    }
}

