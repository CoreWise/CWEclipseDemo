package com.cw.r2000uhfsdk.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


public abstract class ReaderBase {

	private final byte[] OPEN = { (byte) 0xca, (byte) 0xdf, 0x05, 0x36, 0x00, (byte) 0xe3 };
	private final byte[] CLOSE = { (byte) 0xca, (byte) 0xdf, 0x05, 0x36, 0x01, (byte) 0xe3 };
	private WaitThread mWaitThread = null;
	private InputStream mInStream = null;
	private OutputStream mOutStream = null;

	/**
	 * 连接丢失。
	 */
	public abstract void onLostConnect();

	/**
	 * 可重写函数，解析到一包数据后会调用此函数。
	 * 
	 * @param msgTran
	 *            解析到的包
	 */
	public abstract void analyData(MessageTran msgTran);

	// 记录未处理的接收数据，主要考虑接收数据分段
	private byte[] m_btAryBuffer = new byte[4096];
	// 记录未处理数据的有效长度
	private int m_nLength = 0;

	/**
	 * 带参构造函数，构造一个带输入、输出流的Reader。
	 * 
	 * @param in
	 *            输入流
	 * @param out
	 *            输出流
	 */
	public ReaderBase(InputStream in, OutputStream out) {
		this.mInStream = in;
		this.mOutStream = out;

		StartWait();
	}

	public boolean IsAlive() {
		return mWaitThread != null && mWaitThread.isAlive();
	}

	public void StartWait() {
		mWaitThread = new WaitThread();
		mWaitThread.start();
	}

	/**
	 * 循环接收数据线程。
	 * 
	 * @author Jie
	 */
	private class WaitThread extends Thread {
		private boolean mShouldRunning = true;

		public WaitThread() {
			mShouldRunning = true;
		}

		@Override
		public void run() {
			byte[] btAryBuffer = new byte[4096];
			while (mShouldRunning) {
				try {
					int nLenRead = mInStream.read(btAryBuffer);

					if (nLenRead > 0) {
						byte[] btAryReceiveData = new byte[nLenRead];
						System.arraycopy(btAryBuffer, 0, btAryReceiveData, 0, nLenRead);
						runReceiveDataCallback(btAryReceiveData);
					}
				} catch (IOException e) {
					onLostConnect();
					return;
				} catch (Exception e) {
					onLostConnect();
					return;
				}
			}
		}

		public void signOut() {
			mShouldRunning = false;
			interrupt();
		}
	};

	/**
	 * 退出接收线程。
	 */
	public final void signOut() {
		mWaitThread.signOut();
		try {
			mInStream.close();
			mOutStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从输入流读取数据后，会调用此函数。
	 * 
	 * @param btAryReceiveData
	 *            接收到的数据
	 */
	private void runReceiveDataCallback(byte[] btAryReceiveData) {
		try {
			reciveData(btAryReceiveData);

			int nCount = btAryReceiveData.length;
			byte[] btAryBuffer = new byte[nCount + m_nLength];

			System.arraycopy(m_btAryBuffer, 0, btAryBuffer, 0, m_nLength);
			System.arraycopy(btAryReceiveData, 0, btAryBuffer, m_nLength, btAryReceiveData.length);
			// Array.Copy(m_btAryBuffer, btAryBuffer, m_nLenth);
			// Array.Copy(btAryReceiveData, 0, btAryBuffer, m_nLenth,
			// btAryReceiveData.Length);

			// 分析接收数据，以0xA0为数据起点，以协议中数据长度为数据终止点
			int nIndex = 0; // 当数据中存在A0时，记录数据的终止点
			int nMarkIndex = 0; // 当数据中不存在A0时，nMarkIndex等于数据组最大索引
			for (int nLoop = 0; nLoop < btAryBuffer.length; nLoop++) {
				if (btAryBuffer.length > nLoop + 1) {
					if (btAryBuffer[nLoop] == HEAD.HEAD) {
						int nLen = btAryBuffer[nLoop + 1] & 0xFF;
						if (nLoop + 1 + nLen < btAryBuffer.length) {
							byte[] btAryAnaly = new byte[nLen + 2];
							System.arraycopy(btAryBuffer, nLoop, btAryAnaly, 0, nLen + 2);
							// Array.Copy(btAryBuffer, nLoop, btAryAnaly, 0,
							// nLen + 2);

							MessageTran msgTran = new MessageTran(btAryAnaly);
							analyData(msgTran);

							nLoop += 1 + nLen;
							nIndex = nLoop + 1;
						} else {
							nLoop += 1 + nLen;
						}
					} else {
						nMarkIndex = nLoop;
					}
				}
			}

			if (nIndex < nMarkIndex) {
				nIndex = nMarkIndex + 1;
			}

			if (nIndex < btAryBuffer.length) {
				m_nLength = btAryBuffer.length - nIndex;
				Arrays.fill(m_btAryBuffer, 0, 4096, (byte) 0);
				System.arraycopy(btAryBuffer, nIndex, m_btAryBuffer, 0, btAryBuffer.length - nIndex);
				// Array.Clear(m_btAryBuffer, 0, 4096);
				// Array.Copy(btAryBuffer, nIndex, m_btAryBuffer, 0,
				// btAryBuffer.Length - nIndex);
			} else {
				m_nLength = 0;
			}
		} catch (Exception e) {

		}
	}

	/**
	 * 可重写函数，接收到数据时会调用此函数。
	 * 
	 * @param btAryReceiveData
	 *            收到的数据
	 */
	public void reciveData(byte[] btAryReceiveData) {
	};

	/**
	 * 可重写函数，发送数据后会调用此函数。
	 * 
	 * @param btArySendData
	 *            发送的数据
	 */
	public void sendData(byte[] btArySendData) {
	};

	/**
	 * 发送数据，使用synchronized()防止并发操作。
	 * 
	 * @param btArySenderData
	 *            要发送的数据
	 * @return 成功 :0, 失败:-1
	 */
	private int sendMessage(byte[] btArySenderData, boolean toPackage) {

		try {
			synchronized (mOutStream) { // 防止并发
				if (toPackage)
					btArySenderData = toStm32Package(btArySenderData);
				mOutStream.write(btArySenderData);
			}
		} catch (IOException e) {
			onLostConnect();
			return -1;
		} catch (Exception e) {
			return -1;
		}

		sendData(btArySenderData);

		return 0;
	}

	private int sendMessage(byte btReadId, byte btCmd) {
		MessageTran msgTran = new MessageTran(btReadId, btCmd);

		return sendMessage(msgTran.getAryTranData(), true);
	}

	private int sendMessage(byte btReadId, byte btCmd, byte[] btAryData) {
		MessageTran msgTran = new MessageTran(btReadId, btCmd, btAryData);

		return sendMessage(msgTran.getAryTranData(), true);
	}

	/**
	 * 复位指定地址的读写器。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int reset(byte btReadId) {

		byte btCmd = CMD.RESET;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 设置串口通讯波特率。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param nIndexBaudrate
	 *            波特率(0x03: 38400bps, 0x04:115200 bps)
	 * @return 成功 :0, 失败:-1
	 */
	public final int setUartBaudrate(byte btReadId, byte nIndexBaudrate) {
		byte btCmd = CMD.SET_UART_BAUDRATE;
		byte[] btAryData = new byte[1];

		btAryData[0] = nIndexBaudrate;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 读取读写器固件版本。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getFirmwareVersion(byte btReadId) {
		byte btCmd = CMD.GET_FIRMWARE_VERSION;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 设置读写器地址。
	 * 
	 * @param btReadId
	 *            原读写器地址(0xFF公用地址)
	 * @param btNewReadId
	 *            新读写器地址,取值范围0-254(0x00–0xFE)
	 * @return 成功 :0, 失败:-1
	 */
	public final int setReaderAddress(byte btReadId, byte btNewReadId) {
		byte btCmd = CMD.SET_READER_ADDRESS;
		byte[] btAryData = new byte[1];

		btAryData[0] = btNewReadId;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 设置读写器工作天线。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btWorkAntenna
	 *            天线号(0x00:天线1, 0x01:天线2, 0x02:天线3, 0x03:天线4)
	 * @return 成功 :0, 失败:-1
	 */
	public final int setWorkAntenna(byte btReadId, byte btWorkAntenna) {
		byte btCmd = CMD.SET_WORK_ANTENNA;
		byte[] btAryData = new byte[1];

		btAryData[0] = btWorkAntenna;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 查询当前天线工作天线。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getWorkAntenna(byte btReadId) {
		byte btCmd = CMD.GET_WORK_ANTENNA;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	public void openUHF() {
		sendMessage(OPEN, false);
	}

	public void closeUHF() {
		sendMessage(CLOSE, false);
	}

	/**
	 * 设置读写器射频输出功率(方式1)。 <br>
	 * ★此命令耗时将超过100mS。 <br>
	 * ★如果需要动态改变射频输出功率，请使用CmdCode_set_temporary_output_power命令，否则将会影响Flash的使用寿命。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btOutputPower
	 *            RF输出功率,取值范围0-33(0x00–0x21), 单位dBm
	 * @return 成功 :0, 失败:-1
	 */
	public final int setOutputPower(byte btReadId, byte btOutputPower) {
		byte btCmd = CMD.SET_OUTPUT_POWER;
		byte[] btAryData = new byte[1];

		btAryData[0] = btOutputPower;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 设置读写器射频输出功率(方式2)。 <br>
	 * ★此命令耗时将超过100mS。 <br>
	 * ★如果需要动态改变射频输出功率，请使用CmdCode_set_temporary_output_power命令，否则将会影响Flash的使用寿命。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btPower1
	 *            RF天线1输出功率,取值范围0-33(0x00–0x21), 单位dBm
	 * @param btPower2
	 *            RF天线2输出功率,取值范围0-33(0x00–0x21), 单位dBm
	 * @param btPower3
	 *            RF天线3输出功率,取值范围0-33(0x00–0x21), 单位dBm
	 * @param btPower4
	 *            RF天线4输出功率,取值范围0-33(0x00–0x21), 单位dBm
	 * @return 成功 :0, 失败:-1
	 */
	public final int setOutputPower(byte btReadId, byte btPower1, byte btPower2, byte btPower3, byte btPower4) {
		byte btCmd = CMD.SET_OUTPUT_POWER;
		byte[] btAryData = new byte[4];

		btAryData[0] = btPower1;
		btAryData[1] = btPower2;
		btAryData[2] = btPower3;
		btAryData[3] = btPower4;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 查询读写器当前输出功率。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getOutputPower(byte btReadId) {
		byte btCmd = CMD.GET_OUTPUT_POWER;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 设置读写器工作频率范围(使用系统默认的频点)。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btRegion
	 *            射频规范(0x01:FCC, 0x02:ETSI, 0x03:CHN)
	 * @param btStartRegion
	 *            频率起始点,
	 * @param btEndRegion
	 *            频率结束点,可以在射频规范的频率范围内再设置跳频的范围. 参数的设置规则为: 1,起始频率与结束频率不能超过射频规范的范围.
	 *            2,起始频率必须低于结束频率. 3,起始频率等于结束频率则定频发射
	 * @return 成功 :0, 失败:-1
	 */
	public final int setFrequencyRegion(byte btReadId, byte btRegion, byte btStartRegion, byte btEndRegion) {
		byte btCmd = CMD.SET_FREQUENCY_REGION;
		byte[] btAryData = new byte[3];

		btAryData[0] = btRegion;
		btAryData[1] = btStartRegion;
		btAryData[2] = btEndRegion;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 设置读写器工作频率范围(用户自定义频谱)。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btFreqInterval
	 *            频点间隔, 频点间隔 = btFreqInterval x 10KHz
	 * @param btChannelQuantity
	 *            频点数量, 包含起始频率的频点数量, 1为以起始频率定频发射. 此参数必须大于0
	 * @param nStartFreq
	 *            起始频率, 单位为KHz. 16进制数高位在前. 例如915000KHz则发送0x0D 0xF6 0x38
	 * @return 成功 :0, 失败:-1
	 */
	public final int setUserDefineFrequency(byte btReadId, byte btFreqInterval, byte btChannelQuantity,
			int nStartFreq) {
		byte btCmd = CMD.SET_FREQUENCY_REGION;
		byte[] btAryFreq = new byte[3];
		byte[] btAryData = new byte[6];

		btAryFreq = Converter.getBytes(nStartFreq, Converter.BIG_ENDIAN);

		btAryData[0] = 4;
		btAryData[1] = btFreqInterval;
		btAryData[2] = btChannelQuantity;
		btAryData[3] = btAryFreq[2];
		btAryData[4] = btAryFreq[1];
		btAryData[5] = btAryFreq[0];

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 查询读写器工作频率范围。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getFrequencyRegion(byte btReadId) {
		byte btCmd = CMD.GET_FREQUENCY_REGION;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 设置蜂鸣器状态。 <br>
	 * ★读到一张标签后蜂鸣器鸣响，会占用大量处理器时间，若此选项打开，将会明显影响到读多标签(防冲突算法)的性能，此选项应作为测试功能选用。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btMode
	 *            操作标签时蜂鸣器状态(0x00:安静, 0x01:每次盘存后鸣响, 0x02:每读到一张标签鸣响)
	 * @return 成功 :0, 失败:-1
	 */
	public final int setBeeperMode(byte btReadId, byte btMode) {
		byte btCmd = CMD.SET_BEEPER_MODE;
		byte[] btAryData = new byte[1];

		btAryData[0] = btMode;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 查询当前设备的工作温度。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getReaderTemperature(byte btReadId) {
		byte btCmd = CMD.GET_READER_TEMPERATURE;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 读取GPIO电平。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int readGpioValue(byte btReadId) {
		byte btCmd = CMD.READ_GPIO_VALUE;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 设置GPIO电平。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btChooseGpio
	 *            GPIO口选择(0x03:设置GPIO3, 0x04:设置GPIO4)
	 * @param btGpioValue
	 *            GPIO设置值(0x00:低电平, 0x01:高电平)
	 * @return 成功 :0, 失败:-1
	 */
	public final int writeGpioValue(byte btReadId, byte btChooseGpio, byte btGpioValue) {
		byte btCmd = CMD.WRITE_GPIO_VALUE;
		byte[] btAryData = new byte[2];

		btAryData[0] = btChooseGpio;
		btAryData[1] = btGpioValue;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 设置天线连接检测器状态。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btDetectorStatus
	 *            状态(0x00:关闭天线连接检测, 其他值:天线连接检测的灵敏度(端口回波损耗值),单位dB. 值越大对端口的阻抗匹配要求越高
	 * @return 成功 :0, 失败:-1
	 */
	public final int setAntConnectionDetector(byte btReadId, byte btDetectorStatus) {
		byte btCmd = CMD.SET_ANT_CONNECTION_DETECTOR;
		byte[] btAryData = new byte[1];

		btAryData[0] = btDetectorStatus;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 读取天线连接检测器状态。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getAntConnectionDetector(byte btReadId) {
		byte btCmd = CMD.GET_ANT_CONNECTION_DETECTOR;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 设置读写器临时射频输出功率。 <br>
	 * 操作成功后输出功率值将不会被保存在内部的Flash中，重新启动或断电后输出功率将恢复至内部Flash中保存的输出功率值。此命令的操作速度非常快，并且不写Flash，从而不影响Flash的使用寿命，适合需要反复切换射频输出功率的应用。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btRfPower
	 *            RF输出功率, 取值范围20-33(0x14–0x21), 单位dBm
	 * @return 成功 :0, 失败:-1
	 */
	public final int setTemporaryOutputPower(byte btReadId, byte btRfPower) {
		byte btCmd = CMD.SET_TEMPORARY_OUTPUT_POWER;
		byte[] btAryData = new byte[1];

		btAryData[0] = btRfPower;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 设置读写器识别码。 <br>
	 * 操作成功后12字节的读写器识别字符串将会保存在内部的Flash中，断电后不丢失。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btAryIdentifier
	 *            12字节的读写器识别字符
	 * @return 成功 :0, 失败:-1
	 */
	public final int setReaderIdentifier(byte btReadId, byte[] btAryIdentifier) {
		byte btCmd = CMD.SET_READER_IDENTIFIER;

		int nResult = sendMessage(btReadId, btCmd, btAryIdentifier);

		return nResult;
	}

	/**
	 * 读取读写器识别码。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getReaderIdentifier(byte btReadId) {
		byte btCmd = CMD.GET_READER_IDENTIFIER;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 设置射频链路的通讯速率。 <br>
	 * 操作成功后读写器会重新启动，配置保存在内部的Flash中，断电后不丢失。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btProfile
	 *            射频链路的通讯速率(0xD0:Tari 25uS,FM0 40KHz, 0xD1:Tari 25uS,Miller 4
	 *            250KHz, 0xD2:Tari 25uS,Miller 4 300KHz, 0xD3:Tari 6.25uS,FM0
	 *            400KHz)
	 * @return 成功 :0, 失败:-1
	 */
	public final int setRfLinkProfile(byte btReadId, byte btProfile) {
		byte btCmd = CMD.SET_RF_LINK_PROFILE;
		byte[] btAryData = new byte[1];

		btAryData[0] = btProfile;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 读取射频链路的通讯速率。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getRfLinkProfile(byte btReadId) {
		byte btCmd = CMD.GET_RF_LINK_PROFILE;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 测量天线端口的回波损耗。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btFrequency
	 *            频率参数, 系统将获取此频点当前工作天线端口的回波损耗值
	 * @return 成功 :0, 失败:-1
	 */
	public final int getRfPortReturnLoss(byte btReadId, byte btFrequency) {
		byte btCmd = CMD.GET_RF_PORT_RETURN_LOSS;
		byte[] btAryData = new byte[1];

		btAryData[0] = btFrequency;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 盘存标签。 <br>
	 * 读写器收到此命令后，进行多标签识别操作。标签数据存入读写器缓存区。 <br>
	 * 注意： <br>
	 * ★将参数设置成255(0xFF)时，将启动专为读少量标签设计的算法。对于少量标的应用来说，效率更高，反应更灵敏，但此参数不适合同时读取大量标签的应用。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btRepeat
	 *            盘存过程重复的次数, Repeat=0xFF则此轮盘存时间为最短时间.
	 *            如果射频区域内只有一张标签,则此轮的盘存约耗时为30-50mS. 一般在四通道机器上快速轮询多个天线时使用此参数值
	 * @return 成功 :0, 失败:-1
	 */
	public final int inventory(byte btReadId, byte btRepeat) {
		byte btCmd = CMD.INVENTORY;
		byte[] btAryData = new byte[1];

		btAryData[0] = btRepeat;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 读标签。 <br>
	 * 注意： <br>
	 * ★相同EPC的标签，若读取的数据不相同，则被视为不同的标签。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btMemBank
	 *            标签存储区域(0x00:RESERVED, 0x01:EPC, 0x02:TID, 0x03:USER)
	 * @param btWordAdd
	 *            读取数据首地址,取值范围请参考标签规格
	 * @param btWordCnt
	 *            读取数据长度,字长,WORD(16 bits)长度. 取值范围请参考标签规格书
	 * @param btAryPassWord
	 *            标签访问密码,4字节
	 * @return 成功 :0, 失败:-1
	 */
	public final int readTag(byte btReadId, byte btMemBank, byte btWordAdd, byte btWordCnt, byte[] btAryPassWord) {
		byte btCmd = CMD.READ_TAG;
		byte[] btAryData = null;
		if (btAryPassWord == null || btAryPassWord.length < 4) {
			btAryPassWord = null;
			btAryData = new byte[3];
		} else {
			btAryData = new byte[3 + 4];
		}

		btAryData[0] = btMemBank;
		btAryData[1] = btWordAdd;
		btAryData[2] = btWordCnt;

		if (btAryPassWord != null) {
			System.arraycopy(btAryPassWord, 0, btAryData, 3, btAryPassWord.length);
		}

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 写标签。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btAryPassWord
	 *            标签访问密码,4字节
	 * @param btMemBank
	 *            标签存储区域(0x00:RESERVED, 0x01:EPC, 0x02:TID, 0x03:USER)
	 * @param btWordAdd
	 *            数据首地址,WORD(16 bits)地址. 写入EPC存储区域一般从0x02开始,该区域前四个字节存放PC+CRC
	 * @param btWordCnt
	 *            WORD(16 bits)长度,数值请参考标签规格
	 * @param btAryData
	 *            写入的数据, btWordCnt*2 字节
	 * @return 成功 :0, 失败:-1
	 */
	public final int writeTag(byte btReadId, byte[] btAryPassWord, byte btMemBank, byte btWordAdd, byte btWordCnt,
			byte[] btAryData) {
		byte btCmd = CMD.WRITE_TAG;
		byte[] btAryBuffer = new byte[btAryData.length + 7];

		System.arraycopy(btAryPassWord, 0, btAryBuffer, 0, btAryPassWord.length);
		// btAryPassWord.CopyTo(btAryBuffer, 0);
		btAryBuffer[4] = btMemBank;
		btAryBuffer[5] = btWordAdd;
		btAryBuffer[6] = btWordCnt;
		System.arraycopy(btAryData, 0, btAryBuffer, 7, btAryData.length);
		// btAryData.CopyTo(btAryBuffer, 7);

		int nResult = sendMessage(btReadId, btCmd, btAryBuffer);

		return nResult;
	}

	/**
	 * 锁定标签。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btAryPassWord
	 *            标签访问密码,4字节
	 * @param btMembank
	 *            操作的数据区域(0x01:User Memory, 0x02:TID Memory, 0x03:EPC Memory,
	 *            0x04:Access Password, 0x05:Kill Password)
	 * @param btLockType
	 *            锁操作类型(0x00:开放, 0x01:锁定, 0x02:永久开放, 0x03:永久锁定)
	 * @return 成功 :0, 失败:-1
	 */
	public final int lockTag(byte btReadId, byte[] btAryPassWord, byte btMemBank, byte btLockType) {
		byte btCmd = CMD.LOCK_TAG;
		byte[] btAryData = new byte[6];

		System.arraycopy(btAryPassWord, 0, btAryData, 0, btAryPassWord.length);
		// btAryPassWord.CopyTo(btAryData, 0);
		btAryData[4] = btMemBank;
		btAryData[5] = btLockType;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 灭活标签。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btAryPassWord
	 *            标签销毁密码,4字节
	 * @return 成功 :0, 失败:-1
	 */
	public final int killTag(byte btReadId, byte[] btAryPassWord) {
		byte btCmd = CMD.KILL_TAG;
		byte[] btAryData = new byte[4];

		System.arraycopy(btAryPassWord, 0, btAryData, 0, btAryPassWord.length);
		// btAryPassWord.CopyTo(btAryData, 0);

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 匹配ACCESS操作的EPC号(匹配一直有效，直到下一次刷新)。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btEpcLen
	 *            EPC长度
	 * @param btAryEpc
	 *            EPC号, 由EpcLen个字节组成
	 * @return 成功 :0, 失败:-1
	 */
	public final int setAccessEpcMatch(byte btReadId, byte btEpcLen, byte[] btAryEpc) {
		byte btCmd = CMD.SET_ACCESS_EPC_MATCH;
		int nLen = (btEpcLen & 0xFF) + 2;
		byte[] btAryData = new byte[nLen];

		btAryData[0] = 0x00;
		btAryData[1] = btEpcLen;
		System.arraycopy(btAryEpc, 0, btAryData, 2, btAryEpc.length);
		// btAryEpc.CopyTo(btAryData, 2);

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 清除EPC匹配。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int cancelAccessEpcMatch(byte btReadId) {
		byte btCmd = CMD.SET_ACCESS_EPC_MATCH;
		byte[] btAryData = new byte[1];
		btAryData[0] = 0x01;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 查询匹配的EPC状态。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getAccessEpcMatch(byte btReadId) {
		byte btCmd = CMD.GET_ACCESS_EPC_MATCH;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 盘存标签(实时上传标签数据)。 <br>
	 * 注意： <br>
	 * ★由于硬件为双CPU架构，主CPU负责轮询标签，副CPU负责数据管理。轮询标签和发送数据并行，互不占用对方的时间，因此串口的数据传输不影响读写器工作的效率。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btRepeat
	 *            盘存过程重复的次数,Repeat=0xFF则此轮盘存时间为最短时间.
	 *            如果射频区域内只有一张标签,则此轮的盘存约耗时为30-50mS. 一般在四通道机器上快速轮询多个天线时使用此参数值
	 * @return 成功 :0, 失败:-1
	 */
	public final int realTimeInventory(byte btReadId, byte btRepeat) {
		byte btCmd = CMD.REAL_TIME_INVENTORY;
		byte[] btAryData = new byte[1];

		btAryData[0] = btRepeat;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 快速轮询多个天线盘存标签。 <br>
	 * 注意： <br>
	 * ★由于硬件为双CPU架构，主CPU负责轮询标签，副CPU负责数据管理。轮询标签和发送数据并行，互不占用对方的时间，因此串口的数据传输不影响读写器工作的效率。
	 * <br>
	 * ★此命令读取大量标签时，效率没有CmdCode_real_time_inventory命令高。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btA
	 *            首先轮询的天线(00–03), 天线号大于三则表示不轮询
	 * @param btStayA
	 *            天线重复轮询的次数
	 * @param btB
	 *            第二个轮询的天线(00–03), 天线号大于三则表示不轮询
	 * @param btStayB
	 *            天线重复轮询的次数
	 * @param btC
	 *            第三个轮询的天线(00–03), 天线号大于三则表示不轮询
	 * @param btStayC
	 *            天线重复轮询的次数
	 * @param btD
	 *            第四个轮询的天线(00–03), 天线号大于三则表示不轮询
	 * @param btStayD
	 *            天线重复轮询的次数
	 * @param btInterval
	 *            天线间的休息时间. 单位是mS. 休息时无射频输出,可降低功耗
	 * @param btRepeat
	 *            重复以上天线切换顺序次数
	 * @return 成功 :0, 失败:-1
	 */
	public final int fastSwitchAntInventory(byte btReadId, byte btA, byte btStayA, byte btB, byte btStayB, byte btC,
			byte btStayC, byte btD, byte btStayD, byte btInterval, byte btRepeat) {

		byte btCmd = CMD.FAST_SWITCH_ANT_INVENTORY;
		byte[] btAryData = new byte[10];

		btAryData[0] = btA;
		btAryData[1] = btStayA;
		btAryData[2] = btB;
		btAryData[3] = btStayB;
		btAryData[4] = btC;
		btAryData[5] = btStayC;
		btAryData[6] = btD;
		btAryData[7] = btStayD;
		btAryData[8] = btInterval;
		btAryData[9] = btRepeat;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 自定义session和target盘存。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btSession
	 *            指定盘存的session
	 * @param btTarget
	 *            指定盘存的Inventoried Flag(0x00:A, 0x01:B)
	 * @param btRepeat
	 *            盘存过程重复的次数
	 * @return 成功 :0, 失败:-1
	 */
	public final int customizedSessionTargetInventory(byte btReadId, byte btSession, byte btTarget, byte btRepeat) {

		byte btCmd = CMD.CUSTOMIZED_SESSION_TARGET_INVENTORY;
		byte[] btAryData = new byte[3];

		btAryData[0] = btSession;
		btAryData[1] = btTarget;
		btAryData[2] = btRepeat;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 设置Monza标签快速读TID。 <br>
	 * 注意： <br>
	 * ★此功能仅对Impinj Monza 标签的部分型号有效。 <br>
	 * ★此功能在识别EPC的同时识别TID，因此大大提高了读TID的效率。 <br>
	 * ★打开此功能后，特定型号的标签会在盘存的过程中将TID打包到EPC中。因此，标签的PC会被修改，原来的PC+EPC变为：修改后的PC+EPC+(EPC的CRC)+TID。
	 * <br>
	 * ★如果在识别TID的过程中出现错误，则上传原来的PC+EPC。 <br>
	 * ★如不需要此功能请将其关闭，避免不必要的时间消耗。 <br>
	 * ★此命令不保存至内部的Flash中，重启后将回复至Flash保存的值。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param blnOpen
	 *            是否打开FastTID
	 * @param blnSave
	 *            是否保存至内部FLASH
	 * @return 成功 :0, 失败:-1
	 */
	public final int setImpinjFastTid(byte btReadId, boolean blnOpen, boolean blnSave) {

		byte btCmd = (blnSave ? CMD.SET_AND_SAVE_IMPINJ_FAST_TID : CMD.SET_IMPINJ_FAST_TID);
		byte[] btAryData = new byte[1];

		btAryData[0] = (byte) (blnOpen ? 0x8D : 0x00);

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 查询当前的快速TID设置。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getImpinjFastTid(byte btReadId) {
		byte btCmd = CMD.GET_IMPINJ_FAST_TID;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 盘存18000-6B标签。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int iso180006BInventory(byte btReadId) {
		byte btCmd = CMD.ISO18000_6B_INVENTORY;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 读18000-6B标签。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btAryUID
	 *            被操作标签的UID, 8 bytes
	 * @param btWordAdd
	 *            要读取的数据首地址
	 * @param btWordCnt
	 *            要读取的数据长度
	 * @return 成功 :0, 失败:-1
	 */
	public final int iso180006BReadTag(byte btReadId, byte[] btAryUID, byte btWordAdd, byte btWordCnt) {
		byte btCmd = CMD.ISO18000_6B_READ_TAG;
		int nLen = btAryUID.length + 2;
		byte[] btAryData = new byte[nLen];

		System.arraycopy(btAryUID, 0, btAryData, 0, btAryUID.length);
		// btAryUID.CopyTo(btAryData, 0);
		btAryData[nLen - 2] = btWordAdd;
		btAryData[nLen - 1] = btWordCnt;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 写18000-6B标签。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btAryUID
	 *            被操作标签的UID, 8 bytes
	 * @param btWordAdd
	 *            写入数据的首地址
	 * @param btWordCnt
	 *            写入数据的长度
	 * @param btAryBuffer
	 *            写入的数据
	 * @return 成功 :0, 失败:-1
	 */
	public final int iso180006BWriteTag(byte btReadId, byte[] btAryUID, byte btWordAdd, byte btWordCnt,
			byte[] btAryBuffer) {
		byte btCmd = CMD.ISO18000_6B_WRITE_TAG;
		int nLen = btAryUID.length + 2 + btAryBuffer.length;
		byte[] btAryData = new byte[nLen];

		System.arraycopy(btAryUID, 0, btAryData, 0, btAryUID.length);
		// btAryUID.CopyTo(btAryData, 0);
		btAryData[btAryUID.length] = btWordAdd;
		btAryData[btAryUID.length + 1] = btWordCnt;
		System.arraycopy(btAryBuffer, 0, btAryData, btAryUID.length + 2, btAryBuffer.length);
		// btAryBuffer.CopyTo(btAryData, btAryUID.length + 2);

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 锁定18000-6B标签。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btAryUID
	 *            被操作标签的UID, 8 bytes
	 * @param btWordAdd
	 *            被锁定的地址
	 * @return 成功 :0, 失败:-1
	 */
	public final int iso180006BLockTag(byte btReadId, byte[] btAryUID, byte btWordAdd) {
		byte btCmd = CMD.ISO18000_6B_LOCK_TAG;
		int nLen = btAryUID.length + 1;
		byte[] btAryData = new byte[nLen];

		System.arraycopy(btAryUID, 0, btAryData, 0, btAryUID.length);
		// btAryUID.CopyTo(btAryData, 0);
		btAryData[nLen - 1] = btWordAdd;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 查询18000-6B标签。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @param btAryUID
	 *            被操作标签的UID, 8 bytes
	 * @param btWordAdd
	 *            要查询的地址
	 * @return 成功 :0, 失败:-1
	 */
	public final int iso180006BQueryLockTag(byte btReadId, byte[] btAryUID, byte btWordAdd) {
		byte btCmd = CMD.ISO18000_6B_QUERY_LOCK_TAG;
		int nLen = btAryUID.length + 1;
		byte[] btAryData = new byte[nLen];

		System.arraycopy(btAryUID, 0, btAryData, 0, btAryUID.length);
		// btAryUID.CopyTo(btAryData, 0);
		btAryData[nLen - 1] = btWordAdd;

		int nResult = sendMessage(btReadId, btCmd, btAryData);

		return nResult;
	}

	/**
	 * 提取标签数据并删除缓存。 <br>
	 * 注意： <br>
	 * ★命令完成后，缓存中的数据并不丢失，可以多次提取。 <br>
	 * ★若再次运行CmdCode_inventory 命令，则盘存到的标签将累计存入缓存。 <br>
	 * ★若再次运行其他的18000-6C命令，缓存中的数据将被清空。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getInventoryBuffer(byte btReadId) {
		byte btCmd = CMD.GET_INVENTORY_BUFFER;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 提取标签数据保留缓存备份。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getAndResetInventoryBuffer(byte btReadId) {
		byte btCmd = CMD.GET_AND_RESET_INVENTORY_BUFFER;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 查询缓存中已读标签个数。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int getInventoryBufferTagCount(byte btReadId) {
		byte btCmd = CMD.GET_INVENTORY_BUFFER_TAG_COUNT;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 清空标签数据缓存。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int resetInventoryBuffer(byte btReadId) {
		byte btCmd = CMD.RESET_INVENTORY_BUFFER;

		int nResult = sendMessage(btReadId, btCmd);

		return nResult;
	}

	/**
	 * 发送裸数据。
	 * 
	 * @param btReadId
	 *            读写器地址(0xFF公用地址)
	 * @return 成功 :0, 失败:-1
	 */
	public final int sendBuffer(byte[] btAryBuf) {

		int nResult = sendMessage(btAryBuf, false);

		return nResult;
	}

	/**
	 * stm32指令封包
	 * 
	 * @param rawData
	 *            原始数据
	 * @return 封包后的数据
	 */
	public byte[] toStm32Package(byte[] rawData) {
		int len = rawData.length;
		byte[] heade = { (byte) 0xCA, (byte) 0xDF, (byte) 0x05, (byte) 0x35 };
		byte[] cmd = new byte[heade.length + 2 + len + 1];
		System.arraycopy(heade, 0, cmd, 0, heade.length);
		cmd[heade.length] = (byte) 0;
		cmd[heade.length + 1] = (byte) len;
		System.arraycopy(rawData, 0, cmd, heade.length + 2, len);
		cmd[cmd.length - 1] = (byte) 0xE3;
		return cmd;
	}
}
