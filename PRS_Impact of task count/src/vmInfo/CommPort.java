package vmInfo;

import java.util.ArrayList;
import java.util.List;

public class CommPort 
{
	private final int portId; //接口的ID
	private final double bandwidth; //通信端口的带宽
	private List<CommRecord> completedCommRecordList; //已经完成的通信
	private List<CommRecord> unCompletedCommRecordList; //还未完成的通信
	private List<CommRecord> planCommRecordList; //还未完成的通信
	
	public CommPort(int id, double bw)
	{
		this.portId = id;
		this.bandwidth = bw;
		this.completedCommRecordList = new ArrayList<CommRecord>();
		this.unCompletedCommRecordList = new ArrayList<CommRecord>();
		this.planCommRecordList = new ArrayList<CommRecord>();
	}
	
	/**获取接口的Id*/
	public int getPortId(){ return portId;}
	
	/**获取带宽的大小*/
	public double getBandwidth(){ return bandwidth;}
	
	/**获取已经完成的通信记录*/
	public List<CommRecord> getComCommRecordList(){ return completedCommRecordList;}
	
	/**获取未完成的通信记录*/
	public List<CommRecord> getUnComCommRecordList(){ return unCompletedCommRecordList;}
	
	/**获取计划的通信记录*/
	public List<CommRecord> getPlanCommRecordList() { return planCommRecordList;}
}
