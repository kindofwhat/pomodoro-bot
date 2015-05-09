package pomodoro;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackUser;

import java.io.StringWriter;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.util.stream.Collectors.toMap;

/**
 * Created by christian on 25.04.15.
 */
public class PomodoroTask extends TimerTask {
    private final PomodoroBot bot;
    private Map<UserAndChanndel, LocalTime> pomodoros= new HashMap<>();


    private class UserAndChanndel {
        private final SlackUser user;
        private final SlackChannel channel;

        public UserAndChanndel(SlackUser user, SlackChannel channel) {
            this.channel = channel;
            this.user = user;
        }

        public SlackUser getUser() {
            return user;
        }

        public SlackChannel getChannel() {
            return channel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserAndChanndel that = (UserAndChanndel) o;

            return !(user != null ? !user.equals(that.user) : that.user != null);

        }

        @Override
        public int hashCode() {
            return user != null ? user.hashCode() : 0;
        }
    }

    public PomodoroTask(PomodoroBot bot) {
        this.bot = bot;
    }

    public void showStatus(SlackUser sender, SlackChannel channel) {
        StringWriter writer = new StringWriter();
        LocalTime now = LocalTime.now();
        pomodoros.forEach((userAndChanndel, until) -> writer.append(userAndChanndel.getUser().getRealName())
                .append(": is in a pomodoro for another ").append("" + ChronoUnit.MINUTES.between(now,until)).append(" minutes"));
        bot.sendMessage(writer.toString(), channel);

    }


    public Date startPomodoro(SlackUser user, SlackChannel answerChannel, Integer minutesDuration) {
        Date now = new Date();
        pomodoros.put(new UserAndChanndel(user, answerChannel), LocalTime.now().plusMinutes(minutesDuration));
        bot.communicateStart(user, answerChannel);
        return  now;
    }

    public void stopPomodoro(SlackUser user, SlackChannel answerChannel) {
        pomodoros.remove(new UserAndChanndel(user, answerChannel));
        bot.communicateStop(user,answerChannel);
    }

    @Override
    public void run() {
        System.out.println("running with " + pomodoros.size());
        LocalTime now = LocalTime.now();
        Map<UserAndChanndel,LocalTime> toBeExpired =  pomodoros.entrySet().stream().filter(entry -> entry.getValue().isBefore(now)).collect(toMap(Map.Entry::getKey,Map.Entry::getValue));
        toBeExpired.forEach((userAndChanndel, localDateTime) -> bot.communicateStop(userAndChanndel.getUser(), userAndChanndel.getChannel()));
        pomodoros.keySet().removeAll(toBeExpired.keySet());
    }
}
