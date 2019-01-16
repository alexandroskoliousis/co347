import java.io.*;

public class MessageEntry implements Comparable<MessageEntry> {
	
	private String message;
	private int priority;
	private long timestamp;
	
	public MessageEntry (String message, int priority, long timestamp) {
		this.message   =   message;
		this.priority  =  priority;
		this.timestamp = timestamp;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public String getMessage() {
		return message;
	}
	
	public int compareTo (MessageEntry m) {
		int result;
		result = this.priority - m.priority;
		if (result == 0)
			return (int) (this.timestamp - m.timestamp);
		return result;
	}
}
