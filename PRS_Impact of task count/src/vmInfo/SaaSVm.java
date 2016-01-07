package vmInfo;

import java.util.ArrayList;
import java.util.List;
import workflow.WTask;

public class SaaSVm 
{
	private final int vmID; //�������ID
	private final String vmType; //�����������
	
	private final double vmPrice; //������ļ۸�
	private double totalCost; //��������������ڵķ���
	
	private final double startWorkTime; //�������ʼ������ʱ��
	private double endWorkTime; //���������������ʱ��
	private double realWorkTime; //�����ʵ�ʹ�����ʱ��
	private double idleTime; //��������е�ʱ��
	
	private final double executionTimeFactor; //����ִ��ʱ���Ӱ������
	private double finishTime; //�������ִ�������ʱ��
	private double realFinishTime; //���������ʱ��
	private double readyTime; //���������ʱ��
	
	private boolean status; //�����״̬
		
	private List<WTask> WTaskList; //�������ɵ���������	
	private List<WTask> waitWTaskList; //�ȴ������б�	
	private WTask waitingWTask; //���ڵȴ�������     
	private WTask executingWTask; //����ִ������
	
	private List<CommPort> commPortList; //�������ͨ�Žӿ�
	
	public SaaSVm(int id, String type, double startTime, 
			double price, double factor, List<CommPort> list)
	{
		this.vmID = id;
		this.vmType = type;
		this.vmPrice = price;
		this.executionTimeFactor = factor;
		
		this.totalCost = 0;
		this.startWorkTime = startTime;
		this.endWorkTime = startTime + 3600; //ʱ������Ϊ��λ
		this.realWorkTime = 0;
		this.idleTime = 0;
		
		this.finishTime = -1;
		this.realFinishTime = -1;
		this.readyTime = startTime; //�����������ʱ��������ľ���ʱ����ǿ�ʼ����ʱ��
		
		this.status = true;
		this.WTaskList = new ArrayList<WTask>();
		this.waitWTaskList = new ArrayList<WTask>();
		this.waitingWTask = new WTask();
		this.executingWTask = new WTask();
		
		this.commPortList = list;
	}
	
	/**��ȡ�������ͨ�Žӿ�*/
	public List<CommPort> getCommPortList(){return commPortList;}
	
	/**��ȡ�������ID*/
	public int getVmID(){return vmID;}
	
	/**��ȡ�����������*/
	public String getVmType(){return vmType;}
	
	/**��ȡ������ļ۸�*/
	public double getVmPrice(){return vmPrice;}		
	
	/**��ȡ�����������ִ��ʱ���Ӱ������*/
	public double getVmFactor(){return executionTimeFactor;}
	
	/**��ȡ�������ʼ������ʱ��*/
	public double getVmStartWorkTime(){return startWorkTime;}
	
	/**��ȡ��������ܷ���*/
	public double getTotalCost(){return totalCost;}
	/**������������ܷ���*/
	public void setTotalCost(double add)
	{
		this.totalCost = add;
	}
		
	/**��ȡ������Ľ���ʱ��*/
	public double getEndWorkTime(){return endWorkTime;}
	/**����������Ľ���ʱ��*/
	public void setEndWorkTime(double endTime)
	{
		this.endWorkTime = endTime;
	}
	
	/**��ȡ����������Ĺ���ʱ��*/
	public double getRealWorkTime(){return realWorkTime;}
	/**��������������Ĺ���ʱ��*/
	public void updateRealWorkTime(double workTime)
	{
		this.realWorkTime += workTime;
	}
	
	/**��ȡ������Ŀ���ʱ��*/
	public double getIdleTime(){return idleTime;}
	/**����������Ŀ���ʱ��*/
	public void updateIdleTime(double idleTime)
	{
		this.idleTime += idleTime;
	}
	
	/**��ȡ�����������������ʱ��*/
	public double getFinishTime(){return finishTime;}
	/**���������������������ʱ��*/
	public void setFinishTime(double finishTime)
	{
		this.finishTime = finishTime;
	}
	
	/**��ȡ�����������������ʱ��*/
	public double getRealFinishTime(){return realFinishTime;}
	/**���������������������ʱ��*/
	public void setRealFinishTime(double rFinishTime)
	{
		this.realFinishTime = rFinishTime;
	}
	
	/**��ȡ������ľ���ʱ��*/
	public double getReadyTime(){return readyTime;}
	/**����������ľ���ʱ��*/
	public void setReadyTime(double readyTime)
	{
		this.readyTime = readyTime;
	}
	
	/**��ȡ�������״̬*/
	public boolean getVmStatus(){return status;}
	/**�����������״̬*/
	public void setVmStatus(boolean status)
	{
		this.status = status;
	}
	
	/**��ȡ������������������б�*/
	public List<WTask> getWTaskList(){return WTaskList;}
	
	/**��ȡ����������ڵȴ��������б�*/
	public List<WTask> getWaitWTaskList(){return waitWTaskList;}
	
	/**��ȡ����������ڵȴ�������*/
	public WTask getWaitingWTask(){return waitingWTask;}
	/**��������������ڵȴ�������*/
	public void setWaitingWTask(WTask task)
	{
		this.waitingWTask = task;
	}
	
	/**��ȡ�����������ִ�е�����*/
	public WTask getExecutingWTask(){return executingWTask;}
	/**���������������ִ�е�����*/
	public void setExecutingWTask(WTask task)
	{
		this.executingWTask = task;
	}
		
}
