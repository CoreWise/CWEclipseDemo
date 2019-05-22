package android_serialport_api;

public interface LooperBuffer {
	void add(byte[] buffer);
	byte[] getFullPacket();
}
