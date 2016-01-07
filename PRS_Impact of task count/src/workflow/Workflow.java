package workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Workflow implements Serializable, Cloneable
{
	private int workflowId;
	private final String workflowName;
	private int arrivalTime;
	private double makespan;
	private int deadline;
	
	private List<WTask> taskList; //�ڵ��б�
	private boolean startedFlag;  //�������Ƿ��Ѿ���ʼ����
	private double finishTime;       //�����������ʱ��  int
	private boolean successfulOrNot; //�������Ƿ񱻳ɹ�ִ��
	
	public Workflow(int workFlowId, String name, int arrivalTime, double makespan, int deadline)
	{
		this.workflowId = workFlowId;
		this.workflowName = name;
		this.arrivalTime = arrivalTime;
		this.makespan = makespan;
		this.deadline = deadline;
		
		this.taskList = new ArrayList<WTask>(); //��ʼ�������б�
		this.startedFlag = false;
		this.finishTime = -1;
		this.successfulOrNot = false;
	}
		
	/**��ȡ��������ID*/
	public int getWorkflowId(){return workflowId;}
	/**���ù�������ID*/
	public void setWorkflowId(int workflowId)
	{
		this.workflowId = workflowId;
	}
	
	/**��ȡ������������*/
	public String getWorkflowName(){return workflowName;}
	
	/**��ȡ�������ĵ���ʱ��*/
	public int getArrivalTime(){return arrivalTime;}
	/**���ù������ĵ���ʱ��*/
	public void setArrivalTime(int arrivalTime)
	{
		this.arrivalTime = arrivalTime;
	}
	
	/**��ȡ�������Ŀ���*/
	public double getMakespan(){return makespan;}
	/**���ù������Ŀ���*/
	public void setMakespan(double makespan)
	{
		this.makespan = makespan;
	}
	
	/**��ȡ�������Ľ�ֹ��*/
	public int getDeadline(){return deadline;}
	/**���ù������Ľ�ֹ��*/
	public void setDeadline(int deadline)
	{
		this.deadline = deadline;
	}
	
	/**��ȡ�ù����������񼯺�*/
	public List<WTask> getTaskList(){return taskList;}
	/**���øù����������񼯺�*/
	public void setTaskList(List<WTask> list)
	{
		this.taskList = list;
	}
	
	/**��ȡ�������Ѿ���ʼ���õı�־*/
	public boolean getStartedFlag(){return startedFlag;}
	/**���ù������Ѿ���ʼ���õı�־*/
	public void setStartedFlag(boolean startedFlag)
	{
		this.startedFlag = startedFlag;
	}
	
	/**��ȡ�����������ʱ��*/
	public double getFinishTime(){return finishTime;}
	/**���ù����������ʱ��*/
	public void setFinishTime(double finishTime)
	{
		this.finishTime = finishTime;
	}
	
	/**��ȡ�������Ƿ��ڽ�ֹ�������*/
	public boolean getSuccessfulOrNot(){return successfulOrNot;}
	/**���ù������Ƿ��ڽ�ֹ�������*/
	public void setSuccessfulOrNot(boolean successfulOrNot)
	{
		this.successfulOrNot = successfulOrNot;
	}
}