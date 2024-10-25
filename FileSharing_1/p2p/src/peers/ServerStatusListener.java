package p2p.src.peers;

public interface ServerStatusListener {
	void onStatusUpdate(String message);
	void onError(String message);
}
