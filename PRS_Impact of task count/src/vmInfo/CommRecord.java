package vmInfo;

public class CommRecord 
{
	private final SaaSVm sendVm; //发送数据的虚拟机
	private final SaaSVm receiveVm; //接收数据的虚拟机
	private final double dataSize; //数据的大小
	private final double bandwidth; //带宽
	private final double startCommTime; //开始传输时间
	private final double endCommTime; //传输的结束时间
	
	public CommRecord(SaaSVm seVM, SaaSVm reVM, double daSize, 
			double bandwidth, double startTime, double endTime)
	{
		this.sendVm = seVM;
		this.receiveVm = reVM;
		this.dataSize = daSize;
		this.bandwidth = bandwidth;
		this.startCommTime = startTime;
		this.endCommTime = endTime;
	}
	
	/**获取发送信息的虚拟机*/
	public SaaSVm getSendVm(){ return sendVm;}
	
	/**获取接收信息的虚拟机*/
	public SaaSVm getReceiveVm(){ return receiveVm;}
	
	/**获取传输数据的大小*/
	public double getDataSize(){ return dataSize;}
	
	/**获取传输数据的带宽*/
	public double getBandwidth(){ return bandwidth;}
	
	/**获取传输的开始时间*/
	public double getStartCommTime(){ return startCommTime;}
	
	/**获取传输的结束时间*/
	public double getEndCommTime(){ return endCommTime;}
}
