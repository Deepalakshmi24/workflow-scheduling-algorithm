package vmInfo;

public class CommRecord 
{
	private final SaaSVm sendVm; //�������ݵ������
	private final SaaSVm receiveVm; //�������ݵ������
	private final double dataSize; //���ݵĴ�С
	private final double bandwidth; //����
	private final double startCommTime; //��ʼ����ʱ��
	private final double endCommTime; //����Ľ���ʱ��
	
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
	
	/**��ȡ������Ϣ�������*/
	public SaaSVm getSendVm(){ return sendVm;}
	
	/**��ȡ������Ϣ�������*/
	public SaaSVm getReceiveVm(){ return receiveVm;}
	
	/**��ȡ�������ݵĴ�С*/
	public double getDataSize(){ return dataSize;}
	
	/**��ȡ�������ݵĴ���*/
	public double getBandwidth(){ return bandwidth;}
	
	/**��ȡ����Ŀ�ʼʱ��*/
	public double getStartCommTime(){ return startCommTime;}
	
	/**��ȡ����Ľ���ʱ��*/
	public double getEndCommTime(){ return endCommTime;}
}
