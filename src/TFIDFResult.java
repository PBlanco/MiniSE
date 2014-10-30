
public class TFIDFResult implements Comparable {
	private String id;
	private double score;
	
	public TFIDFResult(String id, double score) {
		this.id = id;
		this.score = score;
	}
	
	@Override
	public int compareTo(Object o) {
		TFIDFResult other = (TFIDFResult)o;
		return (int) Math.floor(this.score - other.score);
	}
}
