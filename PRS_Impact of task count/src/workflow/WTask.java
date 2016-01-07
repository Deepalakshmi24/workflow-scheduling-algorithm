package workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import vmInfo.SaaSVm;

public class WTask implements Serializable
{
	private final String taskId; //任务的Id,从XML中获取
	private int taskWorkFlowId; //工作流的Id,生成时赋予
	
	private final double baseExecutionTime; //基准执行时间
	private double baseStartTime; //用于计算任务的makespan
	private double baseFinishTime; 
	
	private double realBaseExecutionTime; //???考虑随机因素后的基准执行时间
	
	private double realExecutionTime; //任务真正的执行时间
	private double executionTimeWithConfidency; //一定可信度下任务的执行时间
		
	private double realStartTime; //任务真正的开始时间
	private double startTimeWithConfidency; //一定可信度下任务的开始时间
	private double earliestStartTime;
	private double leastStartTime;
	
	private double realFinishTime; //任务真正的结束时间
	private double finishTimeWithConfidency; //一定可信度下任务的结束时间
	private double earliestFinishTime;
	private double leastFinishTime;
			
	private boolean allocatedFlag; //该任务是否配置到虚拟机
	private SaaSVm allocateVm; //放置的虚拟机
	private boolean finishFlag; //该任务是否已经完成
			
	private double priority;
	
	private int PCPNum;
			
	private List<ConstraintWTask> parentTaskList;   //父节点列表
	private List<ConstraintWTask> sucessorTaskList; //子节点列表
	
	private List<Constraint> parentIDList;    //父节点ID列表
	private List<Constraint> successorIDList; //子节点ID列表
	
	public WTask(String taskId, int workflowId, double executionTime)
	{	
		this.taskId = taskId;
		this.taskWorkFlowId = workflowId;
		
		this.baseExecutionTime = executionTime; //用于计算makespan
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
								
		parentTaskList = new ArrayList<ConstraintWTask>(); //初始化父节点列表
		sucessorTaskList = new ArrayList<ConstraintWTask>(); //初始化子节点列表
		
		parentIDList = new ArrayList<Constraint>();    //初始化父节点ID列表
		successorIDList = new ArrayList<Constraint>(); //初始化子节点ID列表	
	}
	
	public WTask()
	{
		this.taskId = "initial";
		this.baseExecutionTime = -1;
		this.baseFinishTime = -1;
		this.realFinishTime = -1;
	}
	
	/**获取任务的ID*/
	public String getTaskId(){return taskId;}
	
	/**获取任务所在工作流的ID*/
	public int getTaskWorkFlowId(){return taskWorkFlowId;}
	/**设置任务所在工作流的ID*/
	public void setTaskWorkFlowId(int workFlowId)
	{
		this.taskWorkFlowId = workFlowId;
	}
	
	/**获取任务的基准执行时间*/
	public double getBaseExecutionTime(){return baseExecutionTime;}
	
	/**获取任务开始的基准时间，用于计算makespan*/
	public double getBaseStartTime(){return baseStartTime;}
	/**设置任务开始的基准时间，用于计算makespan*/
	public void setBaseStartTime(double startTime)
	{
		this.baseStartTime = startTime;
	}
	
	/**获取任务结束的基准时间，用于计算makespan*/
	public double getBaseFinishTime(){return baseFinishTime;}
	/**设置任务结束的基准时间，用于计算makespan*/
	public void setBaseFinishTime(double bFinishTime)
	{
		this.baseFinishTime = bFinishTime;
	}
	
	/**获取任务真正的基准执行时间*/
	public double getRealBaseExecutionTime(){return realBaseExecutionTime;}
	/**设置任务真正的基准执行时间*/
	public void setRealBaseExecutionTime(double realBaseTime)
	{
		this.realBaseExecutionTime = realBaseTime;
	}
	
	/**获取任务的真正执行时间*/
	public double getRealExecutionTime(){return realExecutionTime;}
	/**设置任务的真正执行时间*/
	public void setRealExecutionTime(double realTime)
	{
		this.realExecutionTime = realTime;
	}
	
	/**获取在一定可信度下任务的执行时间*/
	public double getExecutionTimeWithConfidency(){return executionTimeWithConfidency;}
	/**设置在一定可信度下任务的执行时间*/
	public void setExecutionTimeWithConfidency(double eTimeWithConfidency)
	{
		this.executionTimeWithConfidency = eTimeWithConfidency;
	}
	
	/**获取任务真正的开始时间*/
	public double getRealStartTime(){return realStartTime;}
	/**设置任务真正的开始时间*/
	public void setRealStartTime(double realStartTime)
	{
		this.realStartTime = realStartTime;
	}
	
	/**获取在一定可信度下，任务的开始时间*/
	public double getStartTimeWithConfidency(){return startTimeWithConfidency;}
	/**设置在一定可信度下，任务的开始时间*/
	public void setStartTimeWithConfidency(double startTime)
	{
		this.startTimeWithConfidency = startTime;
	}
	
	/**获取任务的最早开始时间*/
	public double getEarliestStartTime(){return earliestStartTime;}
	/**设置任务的最早开始时间*/
	public void setEarliestStartTime(double earliestStartTime)
	{
		this.earliestStartTime = earliestStartTime;
	}
	
	/**获取任务的最晚开始时间*/
	public double getLeastStartTime(){return leastStartTime;}
	/**设置任务的最晚开始时间*/
	public void setLeastStartTime(double leastStartTime)
	{
		this.leastStartTime = leastStartTime;
	}
	
	/**获取任务的真正完成时间*/
	public double getRealFinishTime(){return realFinishTime;}
	/**设置任务的真正完成时间*/
	public void setRealFinishTime(double realFinishTime)
	{
		this.realFinishTime = realFinishTime;
	}
		
	/**获取在一定可信度下，任务的结束时间*/
	public double getFinishTimeWithConfidency(){return finishTimeWithConfidency;}
	/**设置在一定可信度下，任务的结束时间*/
	public void setFinishTimeWithConfidency(double finishTime)
	{
		this.finishTimeWithConfidency = finishTime;
	}
	
	/**获取任务的最早完成时间*/
	public double getEarliestFinishTime(){return earliestFinishTime;}
	/**设置任务的最早完成时间*/
	public void setEarliestFinishTime(double earliestFinishTime)
	{
		this.earliestFinishTime = earliestFinishTime;
	}
	
	/**获取任务的最晚完成时间*/
	public double getLeastFinishTime(){return leastFinishTime;}
	/**设置任务的最晚完成时间*/
	public void setLeastFinishTime(double leastFinishTime)
	{
		this.leastFinishTime = leastFinishTime;
	}
			
	/**获取任务是否已经被放置的标志*/
	public boolean getAllocatedFlag(){return allocatedFlag;}
	/**设置任务是否已经被放置的标志*/
	public void setAllocatedFlag(boolean allocatedFlag)
	{
		this.allocatedFlag = allocatedFlag;
	}
	
	/**获取任务放置的虚拟机*/
	public SaaSVm getAllocateVm(){return allocateVm;}
	/**设置任务放置的虚拟机*/
	public void setAllocateVm(SaaSVm vm)
	{
		this.allocateVm = vm;
	}
	
	/**获取任务是否已经被完成的标志*/
	public boolean getFinishFlag(){return finishFlag;}
	/**设置任务是否已经被完成的标志*/
	public void setFinishFlag(boolean flag)
	{
		this.finishFlag = flag;
	}
			
	/**获取任务的权重*/
	public double getPriority(){return priority;}
	/**设置任务的权重*/
	public void setPriority(double priority)
	{
		this.priority = priority;
	}
	
	/**获取任务的PCPNum*/
	public int getPCPNum(){return PCPNum;}
	/**设置任务的权重*/
	public void setPCPNum(int num)
	{
		this.PCPNum = num;
	}
	
	/**获取该任务的父任务列表*/
	public List<ConstraintWTask> getParentTaskList(){return parentTaskList;}
	
	/**获取该任务的子任务列表*/
	public List<ConstraintWTask> getSuccessorTaskList(){return sucessorTaskList;}
	
	/**获取该任务的父任务ID的列表*/
	public List<Constraint> getParentIDList(){return parentIDList; }
	
	/**获取该任务的子任务ID的列表*/
	public List<Constraint> getSuccessorIDList(){return successorIDList; }	
}
