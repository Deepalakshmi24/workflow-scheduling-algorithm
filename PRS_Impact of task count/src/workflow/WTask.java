package workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import vmInfo.SaaSVm;

public class WTask implements Serializable
{
	private final String taskId; //�����Id,��XML�л�ȡ
	private int taskWorkFlowId; //��������Id,����ʱ����
	
	private final double baseExecutionTime; //��׼ִ��ʱ��
	private double baseStartTime; //���ڼ��������makespan
	private double baseFinishTime; 
	
	private double realBaseExecutionTime; //???����������غ�Ļ�׼ִ��ʱ��
	
	private double realExecutionTime; //����������ִ��ʱ��
	private double executionTimeWithConfidency; //һ�����Ŷ��������ִ��ʱ��
		
	private double realStartTime; //���������Ŀ�ʼʱ��
	private double startTimeWithConfidency; //һ�����Ŷ�������Ŀ�ʼʱ��
	private double earliestStartTime;
	private double leastStartTime;
	
	private double realFinishTime; //���������Ľ���ʱ��
	private double finishTimeWithConfidency; //һ�����Ŷ�������Ľ���ʱ��
	private double earliestFinishTime;
	private double leastFinishTime;
			
	private boolean allocatedFlag; //�������Ƿ����õ������
	private SaaSVm allocateVm; //���õ������
	private boolean finishFlag; //�������Ƿ��Ѿ����
			
	private double priority;
	
	private int PCPNum;
			
	private List<ConstraintWTask> parentTaskList;   //���ڵ��б�
	private List<ConstraintWTask> sucessorTaskList; //�ӽڵ��б�
	
	private List<Constraint> parentIDList;    //���ڵ�ID�б�
	private List<Constraint> successorIDList; //�ӽڵ�ID�б�
	
	public WTask(String taskId, int workflowId, double executionTime)
	{	
		this.taskId = taskId;
		this.taskWorkFlowId = workflowId;
		
		this.baseExecutionTime = executionTime; //���ڼ���makespan
		this.baseStartTime = -1;
		this.baseFinishTime = -1;
		
		this.realBaseExecutionTime = 0;
		
		this.realExecutionTime = 0;
		this.executionTimeWithConfidency = 0;
		
		this.realStartTime = -1;
		this.startTimeWithConfidency = -1;
		this.earliestStartTime = -1;		
		this.leastStartTime = -1;
		
		this.realFinishTime = -1;
		this.finishTimeWithConfidency = -1;
		this.earliestFinishTime = -1;
		this.leastFinishTime = -1;
								
		this.allocatedFlag = false;
		this.allocateVm = null;
		this.finishFlag = false;
										
		this.priority = -1;			
		this.PCPNum = -1;
								
		parentTaskList = new ArrayList<ConstraintWTask>(); //��ʼ�����ڵ��б�
		sucessorTaskList = new ArrayList<ConstraintWTask>(); //��ʼ���ӽڵ��б�
		
		parentIDList = new ArrayList<Constraint>();    //��ʼ�����ڵ�ID�б�
		successorIDList = new ArrayList<Constraint>(); //��ʼ���ӽڵ�ID�б�	
	}
	
	public WTask()
	{
		this.taskId = "initial";
		this.baseExecutionTime = -1;
		this.baseFinishTime = -1;
		this.realFinishTime = -1;
	}
	
	/**��ȡ�����ID*/
	public String getTaskId(){return taskId;}
	
	/**��ȡ�������ڹ�������ID*/
	public int getTaskWorkFlowId(){return taskWorkFlowId;}
	/**�����������ڹ�������ID*/
	public void setTaskWorkFlowId(int workFlowId)
	{
		this.taskWorkFlowId = workFlowId;
	}
	
	/**��ȡ����Ļ�׼ִ��ʱ��*/
	public double getBaseExecutionTime(){return baseExecutionTime;}
	
	/**��ȡ����ʼ�Ļ�׼ʱ�䣬���ڼ���makespan*/
	public double getBaseStartTime(){return baseStartTime;}
	/**��������ʼ�Ļ�׼ʱ�䣬���ڼ���makespan*/
	public void setBaseStartTime(double startTime)
	{
		this.baseStartTime = startTime;
	}
	
	/**��ȡ��������Ļ�׼ʱ�䣬���ڼ���makespan*/
	public double getBaseFinishTime(){return baseFinishTime;}
	/**������������Ļ�׼ʱ�䣬���ڼ���makespan*/
	public void setBaseFinishTime(double bFinishTime)
	{
		this.baseFinishTime = bFinishTime;
	}
	
	/**��ȡ���������Ļ�׼ִ��ʱ��*/
	public double getRealBaseExecutionTime(){return realBaseExecutionTime;}
	/**�������������Ļ�׼ִ��ʱ��*/
	public void setRealBaseExecutionTime(double realBaseTime)
	{
		this.realBaseExecutionTime = realBaseTime;
	}
	
	/**��ȡ���������ִ��ʱ��*/
	public double getRealExecutionTime(){return realExecutionTime;}
	/**�������������ִ��ʱ��*/
	public void setRealExecutionTime(double realTime)
	{
		this.realExecutionTime = realTime;
	}
	
	/**��ȡ��һ�����Ŷ��������ִ��ʱ��*/
	public double getExecutionTimeWithConfidency(){return executionTimeWithConfidency;}
	/**������һ�����Ŷ��������ִ��ʱ��*/
	public void setExecutionTimeWithConfidency(double eTimeWithConfidency)
	{
		this.executionTimeWithConfidency = eTimeWithConfidency;
	}
	
	/**��ȡ���������Ŀ�ʼʱ��*/
	public double getRealStartTime(){return realStartTime;}
	/**�������������Ŀ�ʼʱ��*/
	public void setRealStartTime(double realStartTime)
	{
		this.realStartTime = realStartTime;
	}
	
	/**��ȡ��һ�����Ŷ��£�����Ŀ�ʼʱ��*/
	public double getStartTimeWithConfidency(){return startTimeWithConfidency;}
	/**������һ�����Ŷ��£�����Ŀ�ʼʱ��*/
	public void setStartTimeWithConfidency(double startTime)
	{
		this.startTimeWithConfidency = startTime;
	}
	
	/**��ȡ��������翪ʼʱ��*/
	public double getEarliestStartTime(){return earliestStartTime;}
	/**������������翪ʼʱ��*/
	public void setEarliestStartTime(double earliestStartTime)
	{
		this.earliestStartTime = earliestStartTime;
	}
	
	/**��ȡ���������ʼʱ��*/
	public double getLeastStartTime(){return leastStartTime;}
	/**�������������ʼʱ��*/
	public void setLeastStartTime(double leastStartTime)
	{
		this.leastStartTime = leastStartTime;
	}
	
	/**��ȡ������������ʱ��*/
	public double getRealFinishTime(){return realFinishTime;}
	/**����������������ʱ��*/
	public void setRealFinishTime(double realFinishTime)
	{
		this.realFinishTime = realFinishTime;
	}
		
	/**��ȡ��һ�����Ŷ��£�����Ľ���ʱ��*/
	public double getFinishTimeWithConfidency(){return finishTimeWithConfidency;}
	/**������һ�����Ŷ��£�����Ľ���ʱ��*/
	public void setFinishTimeWithConfidency(double finishTime)
	{
		this.finishTimeWithConfidency = finishTime;
	}
	
	/**��ȡ������������ʱ��*/
	public double getEarliestFinishTime(){return earliestFinishTime;}
	/**����������������ʱ��*/
	public void setEarliestFinishTime(double earliestFinishTime)
	{
		this.earliestFinishTime = earliestFinishTime;
	}
	
	/**��ȡ������������ʱ��*/
	public double getLeastFinishTime(){return leastFinishTime;}
	/**����������������ʱ��*/
	public void setLeastFinishTime(double leastFinishTime)
	{
		this.leastFinishTime = leastFinishTime;
	}
			
	/**��ȡ�����Ƿ��Ѿ������õı�־*/
	public boolean getAllocatedFlag(){return allocatedFlag;}
	/**���������Ƿ��Ѿ������õı�־*/
	public void setAllocatedFlag(boolean allocatedFlag)
	{
		this.allocatedFlag = allocatedFlag;
	}
	
	/**��ȡ������õ������*/
	public SaaSVm getAllocateVm(){return allocateVm;}
	/**����������õ������*/
	public void setAllocateVm(SaaSVm vm)
	{
		this.allocateVm = vm;
	}
	
	/**��ȡ�����Ƿ��Ѿ�����ɵı�־*/
	public boolean getFinishFlag(){return finishFlag;}
	/**���������Ƿ��Ѿ�����ɵı�־*/
	public void setFinishFlag(boolean flag)
	{
		this.finishFlag = flag;
	}
			
	/**��ȡ�����Ȩ��*/
	public double getPriority(){return priority;}
	/**���������Ȩ��*/
	public void setPriority(double priority)
	{
		this.priority = priority;
	}
	
	/**��ȡ�����PCPNum*/
	public int getPCPNum(){return PCPNum;}
	/**���������Ȩ��*/
	public void setPCPNum(int num)
	{
		this.PCPNum = num;
	}
	
	/**��ȡ������ĸ������б�*/
	public List<ConstraintWTask> getParentTaskList(){return parentTaskList;}
	
	/**��ȡ��������������б�*/
	public List<ConstraintWTask> getSuccessorTaskList(){return sucessorTaskList;}
	
	/**��ȡ������ĸ�����ID���б�*/
	public List<Constraint> getParentIDList(){return parentIDList; }
	
	/**��ȡ�������������ID���б�*/
	public List<Constraint> getSuccessorIDList(){return successorIDList; }	
}
