package com.cw.r2000uhfsdk.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryBuffer {

    public static class InventoryTagMap {
		public String strPC;
		public String strCRC;
		public String strEPC;
		public byte btAntId;
		public String strRSSI;
		public int nReadCount;
		public String strFreq;
		public int nAnt1;
		public int nAnt2;
		public int nAnt3;
		public int nAnt4;
		
		public InventoryTagMap() {
			strPC = "";
			strCRC = "";
			strEPC = "";
			btAntId = 0;
			strRSSI = "";
			nReadCount = 0;
			strFreq = "";
			nAnt1 = 0;
			nAnt2 = 0;
			nAnt3 = 0;
			nAnt4 = 0;
		}

		@Override
		public String toString() {
			return "InventoryTagMap{" +
					"strPC='" + strPC + '\'' +
					", strCRC='" + strCRC + '\'' +
					", strEPC='" + strEPC + '\'' +
					", btAntId=" + btAntId +
					", strRSSI='" + strRSSI + '\'' +
					", nReadCount=" + nReadCount +
					", strFreq='" + strFreq + '\'' +
					", nAnt1=" + nAnt1 +
					", nAnt2=" + nAnt2 +
					", nAnt3=" + nAnt3 +
					", nAnt4=" + nAnt4 +
					'}';
		}
	}

	public byte btRepeat;
	public byte btSession;
	public byte btTarget;
	
	public byte btA, btB, btC, btD;
	public byte btStayA, btStayB, btStayC, btStayD;
	public byte btInterval;
	public byte btFastRepeat;
	
	
	public ArrayList<Byte> lAntenna;
	public boolean bLoopInventory;
	public int nIndexAntenna;
	public int nCommond;
	public boolean bLoopInventoryReal;
	public boolean bLoopCustomizedSession;
	
	public int nTagCount;
	public int nDataCount; //执行一次命令所返回的标签记录条数
	public int nCommandDuration;
	public int nReadRate;
	public int nCurrentAnt;
	public int nTotalRead;
	public Date dtStartInventory;
	public Date dtEndInventory;
	public int nMaxRSSI;
	public int nMinRSSI;
	public Map<String, Integer> dtIndexMap;
	public List<InventoryTagMap> lsTagList;

    public InventoryBuffer() {
		btRepeat = 0x00;
		btFastRepeat = 0x00;
		
		/** 大于3表示不轮询*/
		btA = btB = btC = btD = (byte) 0xFF;
		btStayA = btStayB = btStayC = btStayD = 0x01;
		
		lAntenna = new ArrayList<Byte>();
		bLoopInventory = false;
		nIndexAntenna = 0;
		nCommond = 0;
		bLoopInventoryReal = false;
		
		nTagCount = 0;
		nReadRate = 0;
		nTotalRead = 0;
		dtStartInventory = new Date();
		dtEndInventory = dtStartInventory;
		nMaxRSSI = 0;
		nMinRSSI = 0;
		
		dtIndexMap = new LinkedHashMap<String, Integer>();
		lsTagList = new ArrayList<InventoryTagMap>();


    }

    public final void clearInventoryPar() {
		btRepeat = 0x00;
		lAntenna.clear();
		//bLoopInventory = false;
		nIndexAntenna = 0;
		nCommond = 0;
		bLoopInventoryReal = false;
    }

    public final void clearInventoryResult() {
		nTagCount = 0;
		nReadRate = 0;
		nTotalRead = 0;
		dtStartInventory = new Date();
		dtEndInventory = dtStartInventory;
		nMaxRSSI = 0;
		nMinRSSI = 0;
		clearTagMap();
    }

    public final void clearInventoryRealResult() {
		nTagCount = 0;
		nReadRate = 0;
		nTotalRead = 0;
		dtStartInventory = new Date();
		dtEndInventory = dtStartInventory;
		nMaxRSSI = 0;
		nMinRSSI = 0;
		clearTagMap();
    }
    
    public final void clearTagMap() {
    	dtIndexMap.clear();
		lsTagList.clear();
    }

	@Override
	public String toString() {
		return "InventoryBuffer{" +
				"btRepeat=" + btRepeat +
				", btSession=" + btSession +
				", btTarget=" + btTarget +
				", btA=" + btA +
				", btB=" + btB +
				", btC=" + btC +
				", btD=" + btD +
				", btStayA=" + btStayA +
				", btStayB=" + btStayB +
				", btStayC=" + btStayC +
				", btStayD=" + btStayD +
				", btInterval=" + btInterval +
				", btFastRepeat=" + btFastRepeat +
				", lAntenna=" + lAntenna +
				", bLoopInventory=" + bLoopInventory +
				", nIndexAntenna=" + nIndexAntenna +
				", nCommond=" + nCommond +
				", bLoopInventoryReal=" + bLoopInventoryReal +
				", bLoopCustomizedSession=" + bLoopCustomizedSession +
				", nTagCount=" + nTagCount +
				", nDataCount=" + nDataCount +
				", nCommandDuration=" + nCommandDuration +
				", nReadRate=" + nReadRate +
				", nCurrentAnt=" + nCurrentAnt +
				", nTotalRead=" + nTotalRead +
				", dtStartInventory=" + dtStartInventory +
				", dtEndInventory=" + dtEndInventory +
				", nMaxRSSI=" + nMaxRSSI +
				", nMinRSSI=" + nMinRSSI +
				", dtIndexMap=" + dtIndexMap +
				", lsTagList=" + lsTagList +
				'}';
	}
}
