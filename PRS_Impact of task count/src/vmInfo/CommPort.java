package vmInfo;

import java.util.ArrayList;
import java.util.List;

public class CommPort 
{
	private final int portId; //�ӿڵ�ID
	private final double bandwidth; //ͨ�Ŷ˿ڵĴ���
	private List<CommRecord> completedCommRecordList; //�Ѿ���ɵ�ͨ��
	private List<CommRecord> unCompletedCommRecordList; //��δ��ɵ�ͨ��
	private List<CommRecord> planCommRecordList; //��δ��ɵ�ͨ��
	
	public CommPort(int id, double bw)
	{
		this.portId = id;
		this.bandwidth = bw;
		this.completedCommRecordList = new ArrayList<CommRecord>();
		this.unCompletedCommRecordList = new ArrayList<CommRecord>();
		this.planCommRecordList = new ArrayList<CommRecord>();
	}
	
	/**��ȡ�ӿڵ�Id*/
	public int getPortId(){ return portId;}
	
	/**��ȡ����Ĵ�С*/
	public double getBandwidth(){ return bandwidth;}
	
	/**��ȡ�Ѿ���ɵ�ͨ�ż�¼*/
	public List<CommRecord> getComCommRecordList(){ return completedCommRecordList;}
	
	/**��ȡδ��ɵ�ͨ�ż�¼*/
	public List<CommRecord> getUnComCommRecordList(){ return unCompletedCommRecordList;}
	
	/**��ȡ�ƻ���ͨ�ż�¼*/
	public List<CommRecord> getPlanCommRecordList() { return planCommRecordList;}
}
