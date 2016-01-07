package workflowScheduleAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import share.PerformanceValue;
import share.StaticfinalTags;
import vmInfo.CommPort;
import vmInfo.CommRecord;
import vmInfo.SaaSVm;
import workflow.ConstraintWTask;
import workflow.WTask;
import workflow.Workflow;

/**算法实现：动态伸缩虚拟机；将任务分配到虚拟机上*/
public class ScheduleAlgorithms 
{
	private List<SaaSVm> vmList; //虚拟机集合
	private List<Workflow> workflowList; //工作流集合
	
	public ScheduleAlgorithms() throws Exception
	{
		this.vmList = new ArrayList<SaaSVm>(); //只产生对象，里面为空
		initialVmList(StaticfinalTags.initialVmNum); //初始化系统中的虚拟机
		this.workflowList = new ArrayList<Workflow>(); //初始化工作流列表
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	/**
	 * 调度动态工作流到SaaS虚拟机上
	 */
	public void scheduleDynamicWorkflowToSaaSVmByPRS()
	{
		System.out.println("Algorithm PRS is Started");
		List<Workflow> workflowList = getWorkflowList();
		//计算每个任务在一定可信度下的执行时间
		calculateTaskBaseExecutionTimeWithConfidency(workflowList);
				
		int vmId = getVmList().size(); //全局虚拟机的ID
		List<SaaSVm> activeVmList = new ArrayList<SaaSVm>(); //系统中活跃虚拟机
		activeVmList = getVmList();		
		List<SaaSVm> offVmList = new ArrayList<SaaSVm>(); //系统中已经关闭的虚拟机		
		List<WTask> RH_WTask = new ArrayList<WTask>(); //任务池，放置全局等待任务
		
		for(int i=0; i<workflowList.size(); i++) //对所有工作流进行调度
		{
//			System.out.println("Time: "+workflowList.get(i).getArrivalTime()+" workflow: "+i+" Name: "+workflowList.get(i).getWorkflowName());
			//把同时到达的工作流找出来
			List<Workflow>  workflowsArriveSynchronously = new ArrayList<Workflow>(); //存放同时到达的工作流
			workflowsArriveSynchronously.add(workflowList.get(i));
			StaticfinalTags.currentTime = workflowList.get(i).getArrivalTime(); //设置系统的当前时刻
			for(int k=i+1; k<workflowList.size(); k++) //寻找同时到达的工作流
			{				
				if(workflowList.get(k).getArrivalTime() == workflowList.get(i).getArrivalTime())
				{
					workflowsArriveSynchronously.add(workflowList.get(k));
					i++;
				}
				else
				{
					break;
				}
			}//已经找出同时到达的工作流
						
			for(Workflow rankWorkflow: workflowsArriveSynchronously)
			{//为每个任务分配权重				
				calculateWorkflowTaskLeastTime(rankWorkflow); //计算每个任务的最晚完成时间和最晚开始时间
				rankWTasksForPRS(rankWorkflow); //把任务的最晚完成时间作为该任务的权重
			}//分配权重结束
			
			//对ready任务进行分配
			List<WTask> readyWTaskList = new ArrayList<WTask>();
			readyWTaskList = getReadyWTaskFromNewWorkflows(workflowsArriveSynchronously);
			
			//将就绪任务调度到虚拟机上
			scheduleReadyWTaskToSaaSVM(readyWTaskList, activeVmList, vmId);
			vmId = activeVmList.size() + offVmList.size();
												
			for(Workflow addWorkflow: workflowsArriveSynchronously)
			{//将未调度的任务加入到RH_WTask中
				for(WTask addTask: addWorkflow.getTaskList())
				{
					if(!addTask.getAllocatedFlag()) //任务addTask还未被调度
					{
						RH_WTask.add(addTask);
					}
				}
			}
			
			int nextArrivalTime = Integer.MAX_VALUE; //下一个工作流到达的时刻
			if(i != workflowList.size()-1)
			{
				nextArrivalTime = workflowList.get(i+1).getArrivalTime();
			}
			
			//对虚拟机进行遍历  给turnOffVmTime 和 nextFinishTime赋值
			double nextFinishTime = Integer.MAX_VALUE; //每次需要更新前都要进行这样的赋值
			SaaSVm nextFinishVm = null; //需要更新正在执行任务状态的虚拟机
			double turnOffVmTime = Integer.MAX_VALUE; //每次需要更新前都要进行这样的赋值
			SaaSVm turnOffVm = null; //需要删除的虚拟机
			for(SaaSVm initiatedVm: activeVmList)
			{//对每个虚拟机进行遍历
				double tempFinishTime = initiatedVm.getExecutingWTask().getRealFinishTime();
				if(tempFinishTime != -1)//虚拟机上有正在执行的任务
				{
					if (tempFinishTime < nextFinishTime)//找出  虚拟机上正在执行任务的最早完成时刻
					{
						nextFinishTime = tempFinishTime;
						nextFinishVm = initiatedVm;
					}
				}
				else //？？虚拟机空闲的情况
				{
					//该虚拟机可能关闭的时刻
					double tempTurnOffTime = Integer.MAX_VALUE;
					if((StaticfinalTags.currentTime-initiatedVm.getVmStartWorkTime())%3600 == 0)
					{
						tempTurnOffTime = StaticfinalTags.currentTime;
					}
					else
					{
						int round = (int)((StaticfinalTags.currentTime-initiatedVm.getVmStartWorkTime())/3600);
						tempTurnOffTime = initiatedVm.getVmStartWorkTime()+3600*(round+1);
					}
					if (tempTurnOffTime < turnOffVmTime)//找出 有最早关闭时刻的虚拟机
					{
						turnOffVmTime = tempTurnOffTime;
						turnOffVm = initiatedVm;//需要关闭的虚拟机
					}
					
					if(initiatedVm.getWaitWTaskList().size() != 0)
					{//???删除
						throw new IllegalArgumentException("Error: there exists waiting task on idle VM!");
					}
				}
			}//遍历虚拟机结束
			
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%	
			//对两个相邻到达工作流之间的所有事件进行更新, 包括：（1）虚拟机上正在执行任务的完成，（2） VM空闲且到整个钟，关闭该虚拟机。
			while(nextArrivalTime >= nextFinishTime || 
					nextArrivalTime > turnOffVmTime)
			{								
				if(nextFinishTime <= turnOffVmTime)
				{//更新  下一个完成任务
					WTask finishTask = nextFinishVm.getExecutingWTask();					
					//更新当前的时间, 可由虚拟机上正在执行任务的真正完成时间确定
					StaticfinalTags.currentTime = nextFinishVm.getExecutingWTask().getRealFinishTime();
					if(nextFinishVm.getWaitWTaskList().size() != 0)
					{//虚拟机上有等待任务
						nextFinishVm.getExecutingWTask().setFinishFlag(true);
						WTask nextExecutionTask = nextFinishVm.getWaitWTaskList().get(0);			
						nextFinishVm.setExecutingWTask(nextExecutionTask);
						nextFinishVm.getWaitWTaskList().remove(nextExecutionTask); //将执行的任务从等待队列中移除
					}
					else
					{//虚拟机上不存在等待任务
						nextFinishVm.getExecutingWTask().setFinishFlag(true);
						nextFinishVm.setExecutingWTask(new WTask());
					}
					
/*					if(nextFinishVm.getWaitingWTask().getBaseExecutionTime() != -1)
					{//虚拟机上有正在等待任务
						nextFinishVm.getExecutingWTask().setFinishFlag(true);
						nextFinishVm.setExecutingWTask(nextFinishVm.getWaitingWTask());
						//???改成更新等待队列
						nextFinishVm.setWaitingWTask(new WTask());					
					}
					else
					{//虚拟机上没有正在等待任务
						nextFinishVm.getExecutingWTask().setFinishFlag(true);
						nextFinishVm.setExecutingWTask(new WTask());
					}*/
					
					//找出就绪任务
					List<WTask> readySucessorList = getReadySucessorsInRH(finishTask);					
					//将就绪任务调度到虚拟机上
					scheduleReadyWTaskToSaaSVM(readySucessorList, activeVmList, vmId);
					vmId = activeVmList.size() + offVmList.size();					
					//将调度完的任务从RH中移除
					RH_WTask.removeAll(readySucessorList);									
				}//更新到 任务完成时间  结束
																						
				if(turnOffVmTime < nextFinishTime)
				{//关闭虚拟机, turnOffVmTime为当前的时间
					StaticfinalTags.currentTime = turnOffVmTime; //更新系统当前的时间
					double workTime = turnOffVmTime - turnOffVm.getVmStartWorkTime();					
					double cost = (workTime*turnOffVm.getVmPrice())/3600;
					
					turnOffVm.setEndWorkTime(turnOffVmTime);
					turnOffVm.setTotalCost(cost);
					turnOffVm.setVmStatus(false);
					activeVmList.remove(turnOffVm);
					offVmList.add(turnOffVm);																									
				}//关闭虚拟机结束																								
								
				//对虚拟机进行遍历  给turnOffVmTime 和 nextFinishTime赋值
				nextFinishTime = Integer.MAX_VALUE; //每次需要更新前都要进行这样的赋值
				nextFinishVm = null; //需要更新正在执行任务状态的虚拟机
				turnOffVmTime = Integer.MAX_VALUE; //每次需要更新前都要进行这样的赋值
				turnOffVm = null; //需要删除的虚拟机
				for(SaaSVm initiatedVm: activeVmList)
				{//对每个虚拟机进行遍历
					double tempFinishTime = initiatedVm.getExecutingWTask().getRealFinishTime();
					if(tempFinishTime != -1)//虚拟机上有正在执行的任务
					{
						if (tempFinishTime < nextFinishTime)//找出  虚拟机上正在执行任务的最早完成时刻
						{
							nextFinishTime = tempFinishTime;
							nextFinishVm = initiatedVm;
						}
					}
					else //虚拟机空闲的情况
					{
						//该虚拟机可能关闭的时刻
						double tempTurnOffTime = Integer.MAX_VALUE;
						if((StaticfinalTags.currentTime-initiatedVm.getVmStartWorkTime())%3600 == 0)
						{
							tempTurnOffTime = StaticfinalTags.currentTime;
						}
						else
						{
							int round = (int)((StaticfinalTags.currentTime-initiatedVm.getVmStartWorkTime())/3600);
							tempTurnOffTime = initiatedVm.getVmStartWorkTime()+3600*(round+1);
						}
						if (tempTurnOffTime < turnOffVmTime)//找出 有最早关闭时刻的虚拟机
						{
							turnOffVmTime = tempTurnOffTime;
							turnOffVm = initiatedVm;//需要关闭的虚拟机
						}
						
						if(initiatedVm.getWaitWTaskList().size() != 0)
						{//???删除
							throw new IllegalArgumentException("Error: there exists waiting task on idle VM!");
						}
					}
				}//遍历虚拟机结束	
				
				if(nextArrivalTime==Integer.MAX_VALUE && nextFinishTime==Integer.MAX_VALUE 
						&& turnOffVmTime==Integer.MAX_VALUE)
				{
					break;
				}
			}//end while, 对两个相邻到达工作流之间的所有事件进行更新  一个轮回结束			
		  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		}//end for(int i=0; i<workflowList.size(); i++) //对所有工作流进行调度
		
		//检验调度结果
//		verifyTheScheduleResult(workflowList);
		//检查虚拟机上是否存在同时运行的任务
//		verifyWTaskOnSaaSVm(offVmList);
						
		if(activeVmList.size() != 0)
		{//实验最后应该没有活跃虚拟机
			System.out.println("Error: there exists active VMs at last!");
		}
		
		java.text.DecimalFormat fd = new java.text.DecimalFormat("0.0000");//设定输出结果小数点的位数
		//统计调度指标：(1)费用；(2)资源利用率；(3)波动性
		double totalCost = 0;
		double totalExecutionTime = 0;
		double totalTime = 0;
		
		int taskNum = 0;
		for(Workflow tempWorkflow: workflowList)
		{
			taskNum += tempWorkflow.getTaskList().size();
		}
		
		int taskNumOnVm = 0;
		for(SaaSVm offVm: offVmList)
		{						
			totalCost = totalCost + offVm.getTotalCost();
						
			taskNumOnVm += offVm.getWTaskList().size();
//			double total = offVm.getEndWorkTime()-offVm.getVmStartWorkTime();
//			double cost1 = ((offVm.getEndWorkTime()-offVm.getVmStartWorkTime())*offVm.getVmPrice())/3600;
//			double cost2 = (offVm.getEndWorkTime()-offVm.getVmStartWorkTime())*offVm.getVmPrice()/3600;
//			System.out.println("Total: "+total+" Price: "+offVm.getVmPrice()+" Cost1: "+cost1+" Cost2: "+cost2+" Total Cost: "+offVm.getTotalCost());
						
			double workTime = 0;
			for(WTask task: offVm.getWTaskList())
			{
				workTime = workTime + task.getRealExecutionTime();		
			}
			totalExecutionTime = totalExecutionTime + workTime;
			totalTime = totalTime + offVm.getEndWorkTime()-offVm.getVmStartWorkTime();
			
//			System.out.println("RU for VM "+offVm.getVmID()+" is:"+fd.format((workTime/(offVm.getEndWorkTime()-offVm.getVmStartWorkTime())))+" Task Cost: "+workTime+" Vm Provision: "+(offVm.getEndWorkTime()-offVm.getVmStartWorkTime()));
/*			if((workTime/(offVm.getEndWorkTime()-offVm.getVmStartWorkTime())) < 0.1)
			{
				System.out.println("RU for "+offVm.getVmID()+" : "+fd.format((workTime/(offVm.getEndWorkTime()-offVm.getVmStartWorkTime())))+" Task Cost: "+fd.format(workTime)+" Vm Provision: "+fd.format((offVm.getEndWorkTime()-offVm.getVmStartWorkTime()))+" Task Num: "+offVm.getWTaskList().size());
			}*/
		}		
		double reUtilization = (double)totalExecutionTime/totalTime;
		
/*		if(taskNum != taskNumOnVm)
		{
			System.out.println("Real Num: "+taskNum+" Num on Vm: "+taskNumOnVm);
		}*/
		
		
		double workflowDeviation = 0;
		for(Workflow tempWorkflow: workflowList)
		{
			double maxRealFinishTime = 0;
			double maxFinishTimeWC = 0;
			for(WTask task: tempWorkflow.getTaskList())
			{
				if(task.getRealFinishTime() > maxRealFinishTime)
				{
					maxRealFinishTime = task.getRealFinishTime();
				}
				if(task.getFinishTimeWithConfidency() > maxFinishTimeWC)
				{
					maxFinishTimeWC = task.getFinishTimeWithConfidency();
				}
			}
			workflowDeviation = workflowDeviation + (double)Math.abs((maxFinishTimeWC - maxRealFinishTime))/tempWorkflow.getMakespan();								
		}
		workflowDeviation = workflowDeviation/workflowList.size();
		
		//实验结果的输出	
		PerformanceValue.TotalCost = totalCost;
		PerformanceValue.ResourceUtilization = reUtilization;
		PerformanceValue.deviation = workflowDeviation;
		System.out.println("Total Cost: "+fd.format(totalCost)+" Resource Utilization: "+fd.format(reUtilization)+" Deviation: "+fd.format(workflowDeviation));		
		
		workflowList.clear();
		offVmList.clear();
		activeVmList.clear();		
	}//end public void scheduleDynamicWorkflowToSaaSVmByPRS()
	
	/**检验虚拟机上的任务排序是否合适*/
	public void verifyWTaskOnSaaSVm(List<SaaSVm> offVms)
	{
		for(SaaSVm vm: offVms)
		{
			Collections.sort(vm.getWTaskList(), new WTaskComparatorByRealStartTimeIncrease());
			if(vm.getWTaskList().size() > 1)
			{
				for(int i=0; i<vm.getWTaskList().size()-1; i++)
				{
					if(vm.getWTaskList().get(i).getRealFinishTime() > vm.getWTaskList().get(i+1).getRealStartTime())
					{
						System.out.println("Error: There exist tasks overlap on VMs");
					}
				}
			}			
		}
	}
	
	
	/**验证任务的调度结果是否满足(1)截止期要求;(2)任务的前后约束*/
	public void verifyTheScheduleResult(List<Workflow> workflowList)
	{
		for(Workflow workflow: workflowList)
		{
			for(WTask task: workflow.getTaskList())
			{
//				if(task.getRealFinishTime() > task.getLeastFinishTime() || task.getRealFinishTime() > workflow.getDeadline())
				if(task.getRealFinishTime() > workflow.getDeadline())
				{
					System.out.println("Error: task finish time is larger than workflow's deadline. Task: "+task.getTaskId()+" in workflowId: "+workflow.getWorkflowId()+" workflow name: "+workflow.getWorkflowName());
				}
				for(ConstraintWTask parentCon: task.getParentTaskList())
				{
					if(parentCon.getWTask().getRealFinishTime() > task.getRealStartTime())
					{
						System.out.println("Error: the data constraint is violated. Task: "+task.getTaskId()+" in workflowId: "+workflow.getWorkflowId()+" workflow name: "+workflow.getWorkflowName());
					}
				}				
			}
		}
	}
	
	
	/**根据任务的最晚开始时间进行升序排序*/
	private class WTaskComparatorByLeastStartTimeIncrease implements Comparator<WTask>
	{
		public int compare(WTask cl1, WTask cl2)
		{
			if(cl1.getLeastStartTime() > cl2.getLeastStartTime())
			{
				return 1;
			}
			else if(cl1.getLeastStartTime() < cl2.getLeastStartTime())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
	
	/**根据任务的基准执行时间进行升序排序*/
	private class WTaskComparatorByTaskBaseExecutionTimeIncrease implements Comparator<WTask>
	{
		public int compare(WTask cl1, WTask cl2)
		{
			if(cl1.getBaseExecutionTime() > cl2.getBaseExecutionTime())
			{
				return 1;
			}
			else if(cl1.getBaseExecutionTime() < cl2.getBaseExecutionTime())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
	
	/**根据任务真正开始时间进行升序排序*/
	private class WTaskComparatorByRealStartTimeIncrease implements Comparator<WTask>
	{
		public int compare(WTask cl1, WTask cl2)
		{
			if(cl1.getRealStartTime() > cl2.getRealStartTime())
			{
				return 1;
			}
			else if(cl1.getRealStartTime() < cl2.getRealStartTime())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
	
	/**根据前驱任务完成时间的分位点进行升序排序*/
	private class PredWTaskComparatorByFinishTimeConfiIncrease implements Comparator<ConstraintWTask>
	{
		public int compare(ConstraintWTask cl1, ConstraintWTask cl2)
		{
			if(cl1.getWTask().getFinishTimeWithConfidency() > cl2.getWTask().getFinishTimeWithConfidency())
			{
				return 1;
			}
			else if(cl1.getWTask().getFinishTimeWithConfidency() < cl2.getWTask().getFinishTimeWithConfidency())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
	
	/**根据前驱任务的真正完成时间进行升序排序*/
	private class PredWTaskComparatorByRealFinishTimeIncrease implements Comparator<ConstraintWTask>
	{
		public int compare(ConstraintWTask cl1, ConstraintWTask cl2)
		{
			if(cl1.getWTask().getRealFinishTime() > cl2.getWTask().getRealFinishTime())
			{
				return 1;
			}
			else if(cl1.getWTask().getRealFinishTime() < cl2.getWTask().getRealFinishTime())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
	
	/**获取全局队列中的就绪调度的任务*/
/*	public List<WTask> getReadyScheduledWTaskInRH(List<WTask> WTaskList)
	{
		List<WTask> readyWTaskList = new ArrayList<WTask>();
		for(WTask task: WTaskList)
		{
			if(task.getParentTaskList().size() == 0)
			{//没有父任务的任务为就绪任务
				readyWTaskList.add(task);
			}
			else
			{//有父任务的情况下
				boolean ready = true; //任务urgentTask是否为就绪的标志
				for(ConstraintWTask parentConWTask: task.getParentTaskList())
				{
					if(!parentConWTask.getWTask().getAllocatedFlag())
					{//如果存在未被调度的父任务，那么该任务不是就绪任务
						ready = false;
					}
				}
				if(ready) //如果任务urgentTask为就绪任务的情况
				{
					readyWTaskList.add(task);
					if(task.getAllocatedFlag())
					{
						System.out.println("Error in getReadyScheduledWTaskInRH(List<WTask> WTaskList): the task has been scheduled!");
					}
				}
			}
		}//end for(WTask task: WTaskList)
		return readyWTaskList;		
	}//end getReadyScheduledWTaskInRH(List<WTask> WTaskList) */
	
	/**获取全局等待任务中的就绪任务*/
/*	public List<WTask> getReadyWTaskInRH(List<WTask> WTaskList)
	{
		List<WTask> readyWTaskList = new ArrayList<WTask>();
		for(WTask task: WTaskList)
		{
			if(task.getParentTaskList().size() == 0)
			{//没有父任务的任务为就绪任务
				readyWTaskList.add(task);
			}
			else
			{//有父任务的情况
				boolean ready = true; //任务urgentTask是否为就绪的标志
				for(ConstraintWTask parentConWTask: task.getParentTaskList())
				{
					if(!parentConWTask.getWTask().getFinishFlag())
					{//如果存在未被调度的父任务，那么该任务不是就绪任务
						ready = false;
					}
				}
				if(ready) //如果任务urgentTask为就绪任务的情况
				{
					readyWTaskList.add(task);
					if(task.getAllocatedFlag())
					{
						System.out.println("Error in getReadyWTaskInRH(List<WTask> WTaskList): the task has been scheduled!");
					}
				}
			}
		}//end for(WTask task: WTaskList)
		return readyWTaskList;		
	}//end getReadyWTaskInRH(List<WTask> WTaskList) */
	
	/**找出任务finishTask的就绪后续任务new*/
	public List<WTask> getReadySucessorsInRH(WTask finishTask)
	{
		List<WTask> readyWTaskList = new ArrayList<WTask>();
		for(ConstraintWTask succ: finishTask.getSuccessorTaskList())
		{						
			WTask succWTask = succ.getWTask(); //后续任务
			if(succWTask.getAllocatedFlag())
			{//如果该任务已经调度过，则跳过
				continue;
			}
			boolean ready = true;
			for(ConstraintWTask parent: succWTask.getParentTaskList())
			{//如果所有的前驱任务都已经被调度，那么该任务就成为就绪
//				if(!parent.getWTask().getFinishFlag())
				if(!parent.getWTask().getAllocatedFlag())
				{
					ready = false;
					break;
				}
			}
			if(ready)
			{
				readyWTaskList.add(succWTask);
			}
			
		}
		return readyWTaskList;		
	}
	
	
	/**将就绪任务调度到虚拟机上new*/
	public void scheduleReadyWTaskToSaaSVM(List<WTask> taskList, List<SaaSVm> vmList, int vmID)
	{
		//对就绪任务按照权重进行排序 WTaskComparatorByTaskBaseExecutionTimeIncrease
		Collections.sort(taskList, new WTaskComparatorByLeastStartTimeIncrease()); //对就绪任务进行排序
//		Collections.sort(taskList, new WTaskComparatorByTaskBaseExecutionTimeIncrease()); //对就绪任务进行排序
		for(WTask scheduleTask: taskList) 
		{//对每个任务进行调度
			double minCost = Double.MAX_VALUE;
			double startTimeWithConfi = Double.MAX_VALUE;
			SaaSVm targetVm = null;			
			for(SaaSVm vm: vmList)
			{//寻找虚拟机: 虚拟机为空闲,或者虚拟机的等待任务为空				
				if(!vm.getWaitingWTask().getTaskId().equals("initial"))
				{
					throw new IllegalArgumentException("Error: there exists waiting task!");
				}
				
				//对前驱任务按照完成时间的分位数进行排序排序
				Collections.sort(scheduleTask.getParentTaskList(), new PredWTaskComparatorByFinishTimeConfiIncrease());				
				double startTime = StaticfinalTags.currentTime; //任务放置vm上的开始时间
				for(ConstraintWTask parentCon: scheduleTask.getParentTaskList())
				{//对任务的每个前驱任务进行检测
					SaaSVm parentTaskVm = parentCon.getWTask().getAllocateVm(); //前驱任务所在的虚拟机
					double minFinishTransTime = Double.MAX_VALUE;
					CommPort destinationPort = null;						
					
					//通信瓶颈的带宽
					double minBandwidth = vm.getCommPortList().get(0).getBandwidth();
					if(minBandwidth > StaticfinalTags.bandwidth)
					{
						minBandwidth = StaticfinalTags.bandwidth;
					}
					
					if(!parentTaskVm.equals(vm)) //前驱任务与当前任务不在同一虚拟机上的情况
					{//考虑数据传输: 与前驱任务不在同一虚拟机上的情况, 源主机已经关闭												
						for(CommPort port: vm.getCommPortList())
						{//确定通信接口
							double startCommTime = StaticfinalTags.currentTime;
							if(parentCon.getWTask().getRealFinishTime() > startCommTime)
							{
								startCommTime = parentCon.getWTask().getFinishTimeWithConfidency();
							}
							
							if(port.getUnComCommRecordList().size() == 0 
									& port.getPlanCommRecordList().size() == 0)
							{//接口空闲的情况									
								minFinishTransTime = startCommTime + parentCon.getDataSize()/minBandwidth;
								destinationPort = port;
								break;
							}										
							else if(port.getUnComCommRecordList().size() != 0 
									& port.getPlanCommRecordList().size() == 0)
							{
								double maxEndTime = StaticfinalTags.currentTime;
								List<CommRecord> endRecordList = new ArrayList<CommRecord>();
								for(CommRecord record: port.getUnComCommRecordList())
								{
									if(record.getEndCommTime() <= StaticfinalTags.currentTime)
									{
										endRecordList.add(record);
									}
									else
									{
										maxEndTime = record.getEndCommTime();
									}
								}
								port.getComCommRecordList().addAll(endRecordList);
								port.getUnComCommRecordList().removeAll(endRecordList);
								if(startCommTime < maxEndTime)
								{
									startCommTime = maxEndTime;
								}
								
								double tempFinishTime = startCommTime + parentCon.getDataSize()/minBandwidth;
								if(tempFinishTime < minFinishTransTime)
								{
									minFinishTransTime = tempFinishTime;
									destinationPort = port;
								}																																																							
							}
							else if(port.getPlanCommRecordList().size() != 0)
							{//接口中还有通信的情况
								double maxFinishCommTime = port.getPlanCommRecordList().get(port.getPlanCommRecordList().size()-1).getEndCommTime();
								if(startCommTime < maxFinishCommTime)
								{
									startCommTime = maxFinishCommTime;
								}		
								double tempFinishTime = startCommTime + parentCon.getDataSize()/minBandwidth;
								if(tempFinishTime < minFinishTransTime)
								{
									minFinishTransTime = tempFinishTime;
									destinationPort = port;
								}
							}
						}
												
						double startTransTime = minFinishTransTime - parentCon.getDataSize()/minBandwidth;
						CommRecord planRecord = new CommRecord(null, vm, parentCon.getDataSize(), 
								minBandwidth, startTransTime, minFinishTransTime);
						destinationPort.getPlanCommRecordList().add(planRecord);
					}//测试一个前驱任务结束
					else //前驱任务与当前任务在同一虚拟机上的情况
					{
						if(parentCon.getWTask().getFinishFlag())
						{
							minFinishTransTime = StaticfinalTags.currentTime;
						}
						else
						{
							minFinishTransTime = parentCon.getWTask().getFinishTimeWithConfidency();
						}							
					}
															
					if(minFinishTransTime > startTime)
					{//更新任务的开始时间
						startTime = minFinishTransTime;														
					}
					
					if(!parentCon.getWTask().getAllocatedFlag())
					{//有前驱任务未计算过
						throw new IllegalArgumentException("There exists unfinished parent task!");
					}
				}//确定任务的最早开始时间				
				for(CommPort port: vm.getCommPortList())
				{//清理通信接口中预放置的通信
					port.getPlanCommRecordList().clear();
				}
				
				double tempAvailableTime = StaticfinalTags.currentTime;
				if(!vm.getExecutingWTask().getTaskId().equals("initial"))
				{//？？？修改  虚拟机上还有正在执行任务的情况
					if(startTime < vm.getExecutingWTask().getFinishTimeWithConfidency())
					{
						startTime = vm.getExecutingWTask().getFinishTimeWithConfidency();
					}										
					tempAvailableTime = vm.getExecutingWTask().getFinishTimeWithConfidency();
					
					for(WTask waitTask: vm.getWaitWTaskList())
					{
						if(waitTask.getFinishTimeWithConfidency() > startTime)
						{
							startTime = waitTask.getFinishTimeWithConfidency();
						}
						tempAvailableTime = waitTask.getFinishTimeWithConfidency();
					}						
				}
				
				if((startTime-tempAvailableTime) > StaticfinalTags.maxIdleTime)
				{//超过一定的空闲时间，则跳过
					continue;
				}
								
				double executionTimeWithConfidency = scheduleTask.getExecutionTimeWithConfidency()*vm.getVmFactor();
				double finishTimeWithConfidency = startTime + executionTimeWithConfidency;				
				double cost = (finishTimeWithConfidency - tempAvailableTime)*vm.getVmPrice();						
				if(finishTimeWithConfidency <= scheduleTask.getLeastFinishTime())
				{
					if(cost < minCost)
					{
						minCost = cost;
						targetVm = vm;
						startTimeWithConfi = startTime;
					}
				}				
			}//结束for(SaaSVm vm: vmList) 寻找虚拟机结束
			
			if(targetVm != null)//在启动的虚拟机中找到合适的虚拟机
			{				
				//更新任务scheduleTask的状态					
				double realExecutionTime = scheduleTask.getRealBaseExecutionTime()*targetVm.getVmFactor();				
				double realStartTime = calculateRealStartTimeForWTask(scheduleTask, targetVm);
				double realFinishtTime = realStartTime + realExecutionTime;
				double executionTimeWithConfidency = scheduleTask.getExecutionTimeWithConfidency()*targetVm.getVmFactor();
				double finishTimeWithConfidency = startTimeWithConfi + executionTimeWithConfidency;	
				
				scheduleTask.setAllocatedFlag(true);
				scheduleTask.setAllocateVm(targetVm);
				scheduleTask.setRealStartTime(realStartTime);
				scheduleTask.setRealExecutionTime(realExecutionTime);
				scheduleTask.setRealFinishTime(realFinishtTime);
				
				scheduleTask.setStartTimeWithConfidency(startTimeWithConfi);
				scheduleTask.setFinishTimeWithConfidency(finishTimeWithConfidency);
									
				//更新虚拟机vm的状态
				targetVm.setFinishTime(finishTimeWithConfidency);
				targetVm.setRealFinishTime(realFinishtTime);
				targetVm.setReadyTime(finishTimeWithConfidency);
				targetVm.getWTaskList().add(scheduleTask);
				
				if(targetVm.getExecutingWTask().getTaskId().equals("initial"))
				{//放置到虚拟机上执行
					targetVm.setExecutingWTask(scheduleTask);
				}
				else
				{//放置到虚拟机上等待
					targetVm.getWaitWTaskList().add(scheduleTask);
//					targetVm.setWaitingWTask(scheduleTask);
				}
			}
			else //没找到合适的虚拟机
			{//增加新的虚拟机				
				SaaSVm newVm = addSaaSVmAndAllocateWTask(vmID, scheduleTask); //增加虚拟机
				if(newVm == null){ throw new IllegalArgumentException("Error: can not add a new VM!");}
				vmID++;
				vmList.add(newVm);							
			}
		}//end for(WTask scheduleTask: taskList) //对每个任务进行调度
	}//调度就绪任务结束
			
	/**增加虚拟机，并放置任务new*/
	public SaaSVm addSaaSVmAndAllocateWTask(int vmID, WTask task)
	{		
		SaaSVm newVm = null;	
		double startTime04 = determineStartTimeForReadyTask(task, 4); //考虑数据传输后，任务的最早开始时间				
		double startTime03 = determineStartTimeForReadyTask(task, 3);
		double startTime02 = determineStartTimeForReadyTask(task, 2);
		double startTime01 = determineStartTimeForReadyTask(task, 1);
		double startTime00 = determineStartTimeForReadyTask(task, 0);
		
		double startTime = StaticfinalTags.currentTime; //预计的开始时间
		if(startTime04 + task.getExecutionTimeWithConfidency()*1.6 <= task.getLeastFinishTime())
		{
			newVm = scaleUpVm(vmID, startTime04, 4);
			startTime = startTime04;
		}
		else if(startTime03 + task.getExecutionTimeWithConfidency()*1.4 <= task.getLeastFinishTime())
		{
			newVm = scaleUpVm(vmID, startTime03, 3);
			startTime = startTime03;
		}
		else if(startTime02 + task.getExecutionTimeWithConfidency()*1.2 <= task.getLeastFinishTime())
		{
			newVm = scaleUpVm(vmID, startTime02, 2);
			startTime = startTime02;
		}
		else if(startTime01 + task.getExecutionTimeWithConfidency()*1.2 <= task.getLeastFinishTime())
		{
			newVm = scaleUpVm(vmID, startTime01, 1);
			startTime = startTime01;
		}
		else if(startTime00 + task.getExecutionTimeWithConfidency() <= task.getLeastFinishTime())
		{
			newVm = scaleUpVm(vmID, startTime00, 0);
			startTime = startTime00;
		}
		
		if(newVm == null)
		{//增加一个最大的虚拟机
			newVm = scaleUpVm(vmID, startTime00, 0);
			startTime = startTime00;
		}
		
		//更新任务scheduleTask的状态					
		double realExecutionTime = task.getRealBaseExecutionTime()*newVm.getVmFactor();
		double realStartTime = calculateRealStartTimeForWTask(task, newVm);		
		double realFinishtTime = realStartTime + realExecutionTime;
		double executionTimeWithConfidency = task.getExecutionTimeWithConfidency()*newVm.getVmFactor();
		double finishTimeWithConfidency = startTime + executionTimeWithConfidency;	
		
		task.setAllocatedFlag(true);
		task.setAllocateVm(newVm);
		task.setRealStartTime(realStartTime);
		task.setRealExecutionTime(realExecutionTime);
		task.setRealFinishTime(realFinishtTime);
		
		task.setStartTimeWithConfidency(startTime);
		task.setFinishTimeWithConfidency(finishTimeWithConfidency);
							
		//更新虚拟机vm的状态
		newVm.setFinishTime(finishTimeWithConfidency);
		newVm.setRealFinishTime(realFinishtTime);
		newVm.setReadyTime(finishTimeWithConfidency);
		newVm.getWTaskList().add(task);
		
		if(newVm.getExecutingWTask().getTaskId().equals("initial"))
		{//放置到虚拟机上执行
			newVm.setExecutingWTask(task);
		}
		else
		{//放置到虚拟机上等待
			newVm.setWaitingWTask(task);
			throw new IllegalArgumentException("Error: There exists execution task on new SaaSVm!");
		}
										
		return newVm;
	}
	
	/**确定任务放置在类型虚拟机上的最早开始时间new*/
	public double determineStartTimeForReadyTask(WTask task, int vmType)
	{
		double startTime = StaticfinalTags.currentTime;
		
		double minBandwidth = 0; //通信瓶颈的带宽
		switch(vmType)
		{
		case 0: //产生Extra-Large类型的虚拟机
			minBandwidth = 1200000;
			break;
		case 1:
			minBandwidth = 1000000;
			break;
		case 2:
			minBandwidth = 800000;
			break;
		case 3: 
			minBandwidth = 600000;
			break;
		case 4:
			minBandwidth = 400000;			
			break;		
		default:
			System.out.println("Warming: Only level= 0 1 2 3 4 5 are valid!");
		}
		if(minBandwidth > StaticfinalTags.bandwidth)
		{
			minBandwidth = StaticfinalTags.bandwidth;
		}
		
		List<CommPort> commPortList = new ArrayList<CommPort>();
		for(int i=0; i<StaticfinalTags.portNum; i++) //6GB
		{//创建虚拟机的通信接口
			CommPort port = new CommPort(i, minBandwidth);
			commPortList.add(port);
		}
		
		//对前驱任务按照完成时间的分位数进行排序排序
		Collections.sort(task.getParentTaskList(), new PredWTaskComparatorByFinishTimeConfiIncrease());
		
		for(ConstraintWTask parentCon: task.getParentTaskList())
		{//对任务的每个前驱任务进行检测
			double minFinishTransTime = Double.MAX_VALUE;
			CommPort destinationPort = null;						
									
			for(CommPort port: commPortList)
			{//确定通信接口
				double startCommTime = StaticfinalTags.currentTime; //开始通信时间
				if(parentCon.getWTask().getRealFinishTime() > startCommTime)
				{
					startCommTime = parentCon.getWTask().getFinishTimeWithConfidency();
				}
								
				if(port.getPlanCommRecordList().size() == 0)
				{//接口空闲的情况									
					minFinishTransTime = startCommTime + parentCon.getDataSize()/minBandwidth;
					destinationPort = port;
					break;
				}
				else if(port.getPlanCommRecordList().size() != 0)
				{//接口中还有通信的情况
					double maxFinishCommTime = port.getPlanCommRecordList().get(port.getPlanCommRecordList().size()-1).getEndCommTime();
					if(startCommTime < maxFinishCommTime)
					{
						startCommTime = maxFinishCommTime;
					}		
					double tempFinishTime = startCommTime + parentCon.getDataSize()/minBandwidth;
					if(tempFinishTime < minFinishTransTime)
					{
						minFinishTransTime = tempFinishTime;
						destinationPort = port;
					}
				}
			}
									
			double startTransTime = minFinishTransTime - parentCon.getDataSize()/minBandwidth;
			CommRecord planRecord = new CommRecord(null, null, parentCon.getDataSize(), 
					minBandwidth, startTransTime, minFinishTransTime);
			destinationPort.getPlanCommRecordList().add(planRecord);			
													
			if(minFinishTransTime > startTime)
			{//更新任务的开始时间
				startTime = minFinishTransTime;														
			}
			
			if(!parentCon.getWTask().getAllocatedFlag())
			{//有前驱任务未计算过
				throw new IllegalArgumentException("There exists unfinished parent task!");
			}
		}
		
		return startTime;
	}
	
	/**获取任务放置在虚拟机上的真正开始时间new*/
	public double calculateRealStartTimeForWTask(WTask task, SaaSVm targetVm)
	{
		double realStartTime = StaticfinalTags.currentTime;		
		//对前驱任务按照真正完成时间进行排序排序
		Collections.sort(task.getParentTaskList(), new PredWTaskComparatorByRealFinishTimeIncrease());
		for(ConstraintWTask parentCon: task.getParentTaskList())
		{//对任务的每个前驱任务进行检测
			SaaSVm parentTaskVm = parentCon.getWTask().getAllocateVm(); //前驱任务所在的虚拟机
			double minFinishTransTime = Double.MAX_VALUE;
			CommPort destinationPort = null;						
			
			//通信瓶颈的带宽
			double minBandwidth = targetVm.getCommPortList().get(0).getBandwidth();
			if(minBandwidth > StaticfinalTags.bandwidth)
			{
				minBandwidth = StaticfinalTags.bandwidth;
			}
			
			if(!parentTaskVm.equals(targetVm)) //前驱任务与当前任务不在同一虚拟机上的情况
			{//考虑数据传输: 与前驱任务不在同一虚拟机上的情况, 源主机已经关闭								
				for(CommPort port: targetVm.getCommPortList())
				{//确定通信接口
					double startCommTime = StaticfinalTags.currentTime;
					if(parentCon.getWTask().getRealFinishTime() > startCommTime)
					{
						startCommTime = parentCon.getWTask().getRealFinishTime();
					}
					
					if(port.getUnComCommRecordList().size() == 0 
							& port.getPlanCommRecordList().size() == 0)
					{//接口空闲的情况									
						minFinishTransTime = startCommTime + parentCon.getDataSize()/minBandwidth;
						destinationPort = port;
						break;
					}										
					else if(port.getUnComCommRecordList().size() != 0 
							& port.getPlanCommRecordList().size() == 0)
					{
						double maxEndTime = StaticfinalTags.currentTime;
						for(CommRecord record: port.getUnComCommRecordList())
						{
							if(record.getEndCommTime() > maxEndTime)
							{
								maxEndTime = record.getEndCommTime();
							}
						}
						
						if(startCommTime < maxEndTime)
						{
							startCommTime = maxEndTime;
						}
						
						double tempFinishTime = startCommTime + parentCon.getDataSize()/minBandwidth;
						if(tempFinishTime < minFinishTransTime)
						{
							minFinishTransTime = tempFinishTime;
							destinationPort = port;
						}																																																							
					}
					else if(port.getPlanCommRecordList().size() != 0)
					{//接口中还有通信的情况
						double maxFinishCommTime = port.getPlanCommRecordList().get(port.getPlanCommRecordList().size()-1).getEndCommTime();
						if(startCommTime < maxFinishCommTime)
						{
							startCommTime = maxFinishCommTime;
						}		
						double tempFinishTime = startCommTime + parentCon.getDataSize()/minBandwidth;
						if(tempFinishTime < minFinishTransTime)
						{
							minFinishTransTime = tempFinishTime;
							destinationPort = port;
						}
					}
				}
										
				double startTransTime = minFinishTransTime - parentCon.getDataSize()/minBandwidth;
				CommRecord planRecord = new CommRecord(null, targetVm, parentCon.getDataSize(), 
						minBandwidth, startTransTime, minFinishTransTime);
				destinationPort.getPlanCommRecordList().add(planRecord);
			}//测试一个前驱任务结束
			else //前驱任务与当前任务在同一虚拟机上的情况
			{//不需要数据传输时间
				if(parentCon.getWTask().getFinishFlag())
				{
					minFinishTransTime = StaticfinalTags.currentTime;
				}
				else
				{
					minFinishTransTime = parentCon.getWTask().getRealFinishTime();
				}				
			}
													
			if(minFinishTransTime > realStartTime)
			{//更新任务的开始时间
				realStartTime = minFinishTransTime;														
			}
		}//确定任务的最早开始时间				
		for(CommPort port: targetVm.getCommPortList())
		{//清理通信接口中预放置的通信
			port.getPlanCommRecordList().clear();
		}
		
		if(!targetVm.getExecutingWTask().getTaskId().equals("initial"))
		{//???修改，遍历等待队列中所有任务
			if(realStartTime < targetVm.getExecutingWTask().getRealFinishTime())
			{
				realStartTime = targetVm.getExecutingWTask().getRealFinishTime();
			}
			
			for(WTask waitTask: targetVm.getWaitWTaskList())
			{
				if(waitTask.getRealFinishTime() > realStartTime)
				{
					realStartTime = waitTask.getRealFinishTime();
				}
/*				else
				{//???去掉
					throw new IllegalArgumentException("The real time of task on VM occurs error!");
				}*/					
			}
		}
		
		if(realStartTime < targetVm.getVmStartWorkTime())
		{
			realStartTime = targetVm.getVmStartWorkTime();
		}
				
		return realStartTime;
	}
	
	/**获取新到达工作流的就绪任务，即没有前驱任务的任务new*/
	public List<WTask> getReadyWTaskFromNewWorkflows(List<Workflow> newWorkFlows)
	{
		List<WTask> readyTaskList = new ArrayList<WTask>();
		for(Workflow newWorkflow: newWorkFlows)
		{//对每个新到达工作流进行遍历				
			for(WTask tempWTask: newWorkflow.getTaskList())
			{
				if(tempWTask.getParentTaskList().size() == 0)
				{//对于新到达工作流，没有前驱任务的任务就是就绪任务
					readyTaskList.add(tempWTask);
				}
			}
		}
		return readyTaskList;
	}
	
	/**获取就绪的等待任务w*/
/*	public List<WTask> getReadyWTaskList(List<Workflow> waitWorkFlows)
	{
		List<WTask> readyTaskList = new ArrayList<WTask>(); 
		
		for(Workflow roundWorkflow: waitWorkFlows) 
		{//对每个工作流进行遍历
			for(WTask tempWTask: roundWorkflow.getTaskList())
			{//对工作流中的每个任务进行遍历
				if(!tempWTask.getAllocatedFlag())
				{//判断任务tempWTask是否已经就绪
					boolean ready = true;
					List<ConstraintWTask> parentTaskList = new ArrayList<ConstraintWTask>();
					parentTaskList.addAll(tempWTask.getParentTaskList());
					for(ConstraintWTask parentConstraint: parentTaskList)
					{//???????这里有问题，应该是父任务完成的任务才是就绪任务
						if(!parentConstraint.getWTask().getFinishFlag())
						{
							ready = false;
							break;
						}
					}
					
					if(ready)
					{
						readyTaskList.add(tempWTask);
					}
				}					
			}// end for(WTask tempWTask: tempWorkflow.getTaskList())
		}//end for(Workflow tempWorkflow: waitWorkFlows)		
		return readyTaskList;
	}//获取就绪任务结束 */
	
	/**设置工作流中任务的权重：最晚开始时间作为任务的权重，应该优先考虑调度权重小的任务*/
	public void rankWTasksForPRS(Workflow aWorkflow)
	{
		for(WTask rankTask: aWorkflow.getTaskList())
		{
			double rank = rankTask.getLeastFinishTime();
			rankTask.setPriority(rank);
		}
	}
	
	/**计算工作流中每个任务的最晚结束、开始时间*/
	public void calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	{
		List<WTask> calculatedTaskList = new ArrayList<WTask>(); //已经计算过的任务		
		while(true)
		{
			for(WTask lTask: aWorkflow.getTaskList())//对所有任务进行遍历一次
			{
				if(lTask.getLeastFinishTime() != -1)//如果任务已经计算过，则跳过
				{
					continue;
				}
				
				double executionTime = lTask.getExecutionTimeWithConfidency();
				if(lTask.getSuccessorTaskList().size() == 0)//计算结束任务的开始时间和结束时间
				{
					lTask.setLeastFinishTime(aWorkflow.getDeadline());
					lTask.setLeastStartTime(lTask.getLeastFinishTime()-executionTime);
					calculatedTaskList.add(lTask);
				}
				else//非结束任务,也就是有子任务的任务
				{	
					double leastFinishTime = Double.MAX_VALUE; //最晚开始时间
					boolean unCalculatedSucessor = false; //是否存在未计算的子任务
					for(ConstraintWTask sucessorCon: lTask.getSuccessorTaskList())
					{//对任务的每个后续任务进行检测
						//一个接口通信是可能的最大通信数量
						int round = (sucessorCon.getWTask().getParentTaskList().size()/StaticfinalTags.portNum+2);
						double maxDataSize = 0;
						for(ConstraintWTask predCon: sucessorCon.getWTask().getParentTaskList())
						{
							if(predCon.getDataSize() > maxDataSize)
							{
								maxDataSize = predCon.getDataSize();
							}
						}
						//通信延迟
						double commDelay = ((maxDataSize*round)/StaticfinalTags.bandwidth);
												
						if(sucessorCon.getWTask().getLeastFinishTime() != -1)
						{
							double tempLeastFT = sucessorCon.getWTask().getLeastStartTime() - commDelay;
							if(tempLeastFT < leastFinishTime)
							{
								leastFinishTime = tempLeastFT;
							}
						}
						else
						{//有后续任务未计算过
							unCalculatedSucessor = true;
							break;
						}
					}
					if(unCalculatedSucessor == false)
					{//如果该任务所有子任务都完成了，那么计算它的最晚开始和最晚结束时间
//						leastFinishTime = leastFinishTime - lTask.getBaseExecutionTime();
						lTask.setLeastFinishTime(leastFinishTime);
						lTask.setLeastStartTime(leastFinishTime - executionTime);
						calculatedTaskList.add(lTask);
						if(lTask.getLeastStartTime() < aWorkflow.getArrivalTime())
						{
							System.out.println("The least start time of task is less than its workflow's arrival time!");
/*							if(!aWorkflow.getWorkflowName().equals("Inspiral_30.xml") &&
									!aWorkflow.getWorkflowName().equals("Inspiral_50.xml") &&
									!aWorkflow.getWorkflowName().equals("Inspiral_100.xml"))
							{
								System.out.println("Here");
							}*/
							
//							throw new IllegalArgumentException("The least start time of task is less than its workflow's arrival time!");
						}
					}						
				}//end else
			}//end for(Task task: cWorkflow.getTaskList()) 
			
			if(calculatedTaskList.size() == aWorkflow.getTaskList().size())//计算完的条件
			{
				break;
			}
		}//end while
	}//end calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	
	/**设置任务在一定可信度下的基准执行时间*/
	public static void calculateTaskBaseExecutionTimeWithConfidency(List<Workflow> list)
	{//？？？应该扩展为不同分布
		for(Workflow tempWorkflow: list)
		{//对每个工作流进行遍历
			for(WTask tempTask: tempWorkflow.getTaskList())
			{//对每个工作流中的每个任务进行遍历
				double standardDeviation = tempTask.getBaseExecutionTime()*StaticfinalTags.standardDeviation;				
				double executionTimeWithConfidency = tempTask.getBaseExecutionTime() + standardDeviation*getQuantile(StaticfinalTags.confidency);
				tempTask.setExecutionTimeWithConfidency(executionTimeWithConfidency);
			}
		}
	}
	
	/**获取虚拟机*/
	public List<SaaSVm>  getVmList() {return vmList; }
	
	/**获取工作流*/
	public List<Workflow> getWorkflowList() {return workflowList; }
			
	/**提交工作流集合*/
	public void submitWorkflowList(List<Workflow> list)
	{
		getWorkflowList().addAll(list);
	}
	
	/**初始化系统中的虚拟机: 每种虚拟机的数量尽可能相同*/
	public void initialVmList(int num)
	{
		List<SaaSVm> initialVms = new ArrayList<SaaSVm>(); //初始化
		for(int i=0; i<num; i++)
		{
			int level = i%5;
			SaaSVm initialVm = scaleUpVm(i, 0, level);
			initialVms.add(initialVm);
		}
		getVmList().addAll(initialVms);		
	}
	
	/**
	 * 根据虚拟机的类型产生虚拟机
	 * @param level对应不同类型的虚拟机，如0: Extra-Large; 1: High-CPU; 2: High-Memory; 3: Standard; 4: Micro;
	 * @return 新增的虚拟机
	 */
	public SaaSVm scaleUpVm(int vmId, double startTime, int level)
	{
		SaaSVm tempVm = null;
		switch(level)
		{
		case 0: //产生Extra-Large类型的虚拟机
			List<CommPort> commPortList0 = new ArrayList<CommPort>();
			for(int i=0; i<StaticfinalTags.portNum; i++) //6GB
			{//创建虚拟机的通信接口
				CommPort port = new CommPort(i, 1200000);
				commPortList0.add(port);
			}
//			tempVm = new SaaSVm(vmId, "Extra-Large", startTime, 1.3, 1.0, commPortList0);
			tempVm = new SaaSVm(vmId, "Extra-Large", startTime, 0.8, 1.0, commPortList0);
//			tempVm = new SaaSVm(vmId, "Extra-Large", startTime, 1.3, 1.0);
			break;
		case 1:
			List<CommPort> commPortList1 = new ArrayList<CommPort>();
			for(int i=0; i<StaticfinalTags.portNum; i++) //5GB
			{//创建虚拟机的通信接口
				CommPort port = new CommPort(i, 1000000);
				commPortList1.add(port);
			}			
			tempVm = new SaaSVm(vmId, "High-CPU", startTime, 0.66, 1.2, commPortList1);
			break;
		case 2:
			List<CommPort> commPortList2 = new ArrayList<CommPort>();
			for(int i=0; i<StaticfinalTags.portNum; i++) //4GB
			{//创建虚拟机的通信接口
				CommPort port = new CommPort(i, 800000);
				commPortList2.add(port);
			}			
			tempVm = new SaaSVm(vmId, "High-Memory", startTime, 0.45, 1.2, commPortList2);
			break;
		case 3: 
			List<CommPort> commPortList3 = new ArrayList<CommPort>();
			for(int i=0; i<StaticfinalTags.portNum; i++) //3GB
			{//创建虚拟机的通信接口
				CommPort port = new CommPort(i, 600000);
				commPortList3.add(port);
			}
			tempVm = new SaaSVm(vmId, "Standard", startTime, 0.08, 1.4, commPortList3);	
			break;
		case 4:
			List<CommPort> commPortList4 = new ArrayList<CommPort>();
			for(int i=0; i<StaticfinalTags.portNum; i++) //2GB
			{//创建虚拟机的通信接口
				CommPort port = new CommPort(i, 400000);
				commPortList4.add(port);
			}
			tempVm = new SaaSVm(vmId, "Micro", startTime, 0.02, 1.6, commPortList4);			
			break;		
		default:
			System.out.println("Warming: Only level= 0 1 2 3 4 5 are valid!");
		}		
		return tempVm;
	}
	
	/**获取分位数：only level = 0.7 0.75 0.8 0.85 0.9 0.95 0.99 can be gained！*/
	public static double getQuantile(double level)
	{
		double quantile = 0;
		int aa = (int)(level*100);
		switch(aa)
		{
		case 70:
			quantile = 0.53;
			break;
		case 75:
			quantile = 0.68;
			break;
		case 80:
			quantile = 0.85;
			break;
		case 85:
			quantile = 1.04;
			break;
		case 90:
			quantile = 1.29;
			break;
		case 95:
			quantile = 1.65;
			break;
		case 99:
			quantile = 2.33;
			break;			
		default:
			System.out.println("Warming: Only level= 0.7 0.75 0.8 0.85 0.9 0.95 0.99 are valid!");
		}		
		return quantile;
	}
	
}
