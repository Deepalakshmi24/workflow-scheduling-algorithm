package workflowScheduleAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import share.PerformanceValue;
import share.StaticfinalTags;
import vmInfo.CommPort;
import vmInfo.SaaSVm;
import workflow.Constraint;
import workflow.ConstraintWTask;
import workflow.WTask;
import workflow.Workflow;

public class PCP_RTC_CompareAlgorithm 
{
	private List<SaaSVm> vmList; //虚拟机集合
	private List<Workflow> workflowList; //工作流集合
	
	public PCP_RTC_CompareAlgorithm() throws Exception
	{
		this.vmList = new ArrayList<SaaSVm>(); //只产生对象，里面为空
		initialVmList(StaticfinalTags.initialVmNum); //初始化系统中的虚拟机
		this.workflowList = new ArrayList<Workflow>(); //初始化工作流列表
	}
	
	/**算法RTC的实现，来自Deepak Poola的文章Robust Scheduling of Scientific Workflows with Deadline and Budget Constraints in Clouds*/
	public void scheduleDynamicWorkflowsByRTC()
	{
		System.out.println("Algorithm RTC is Started");
		List<Workflow> workflowList = getWorkflowList();
		calculateTaskBaseExecutionTimeBeforeScheduling(workflowList);
		
//		int vmId = getVmList().size(); //全局虚拟机的ID
		List<SaaSVm> activeVmList = new ArrayList<SaaSVm>(); //系统中活跃虚拟机
		activeVmList = getVmList();		
		List<SaaSVm> offVmList = new ArrayList<SaaSVm>(); //系统中已经关闭的虚拟机
		
		//对每个任务进行调度
		for(int i=0; i<workflowList.size(); i++)
		{
	//		System.out.println("Start WorkflowID: "+i);			
			StaticfinalTags.currentTime = workflowList.get(i).getArrivalTime(); 
			//计算工作流中所有任务的最晚开始、结束时间
			calculateWorkflowTaskLeastTime(workflowList.get(i));			
			//找出工作流所有的PCPs
			searchPCPsForWorkflows(workflowList.get(i));						
			//将所有的PCPs分配到虚拟机上
			allocatePcPsToSaaSVMs(workflowList.get(i), activeVmList, offVmList);			
			
			if(i != workflowList.size()-1)
			{//非最后一个工作流，工作流分配后系统的状态更新到下一个任务到达的时刻	
				StaticfinalTags.currentTime = workflowList.get(i+1).getArrivalTime(); 				
			}
			else
			{//如果对最后一个工作流进行调度，那么将当前时间设置到最大，使得所有虚拟机的状态可以更新
				StaticfinalTags.currentTime = Integer.MAX_VALUE;
			}
			//更新每个任务及虚拟机的真正执行开始时间
			updateWTaskAndVmRealExecutionInformation(workflowList.get(i));
			//更新虚拟机的状态			
			updateActiveVmStatus(activeVmList, offVmList);
//			updateActiveVmStatusNew(activeVmList, offVmList);
		}		
		//统计实验结果
		outputExperimentResult(offVmList, workflowList);	
		
		//检验任务的放置顺序是否正确
//		verifyLogic(workflowList); 
		
		workflowList.clear();
		offVmList.clear();
		activeVmList.clear();				
	}
	
	/**设置任务在调度前使用的基准执行时间：任务的平均执行时间加上标准差*/
	public static void calculateTaskBaseExecutionTimeBeforeScheduling(List<Workflow> list)
	{
		for(Workflow tempWorkflow: list)
		{//对每个工作流进行遍历
			for(WTask tempTask: tempWorkflow.getTaskList())
			{//对每个工作流中的每个任务进行遍历
				double standardDeviation = tempTask.getBaseExecutionTime()*StaticfinalTags.standardDeviation;				
				//任务调度前使用的执行时间参数：执行时间的平均值加上执行时间的标准差
				double executionTimeWithConfidency =  tempTask.getBaseExecutionTime() + standardDeviation;
				tempTask.setExecutionTimeWithConfidency(executionTimeWithConfidency);
			}
		}
	}
	
	public void verifyLogic(List<Workflow> workflowList)
	{
		for(Workflow tempWorkflow: workflowList)
		{
			double maxFinishTimeWC = 0;
			double maxFinishTime = 0;
			for(WTask task: tempWorkflow.getTaskList())
			{
				if(task.getFinishTimeWithConfidency() > maxFinishTimeWC)
				{
					maxFinishTimeWC = task.getFinishTimeWithConfidency();
				}
				if(task.getRealFinishTime() > maxFinishTime)
				{
					maxFinishTime = task.getRealFinishTime();
				}
				
				double maxParentRealFinishTime = 0;
				double maxParentFinishTimeWC = 0;
				for(ConstraintWTask ConWTask: task.getParentTaskList())
				{
					if(ConWTask.getWTask().getFinishTimeWithConfidency() > maxParentFinishTimeWC)
					{
						maxParentFinishTimeWC = ConWTask.getWTask().getFinishTimeWithConfidency();
					}
					if(ConWTask.getWTask().getRealFinishTime() > maxParentRealFinishTime)
					{
						maxParentRealFinishTime = ConWTask.getWTask().getRealFinishTime();
					}
				}
				if(task.getStartTimeWithConfidency() < maxParentFinishTimeWC)
				{
					System.out.println("Error: there exist errors in computing task start time with confidency");
				}
				if(task.getRealStartTime() < maxParentRealFinishTime)
				{
					System.out.println("Error: there exist errors in computing task real start time");
				}
			}
			
/*			if(maxFinishTimeWC > tempWorkflow.getDeadline())
			{
				System.out.println(tempWorkflow.getWorkflowId()+" "+tempWorkflow.getWorkflowName()+" "+(maxFinishTimeWC-tempWorkflow.getDeadline())+" Error: workflow's finish time with confidency is larger than its deadline");
			}
			if(maxFinishTime > tempWorkflow.getDeadline())
			{
				System.out.println(tempWorkflow.getWorkflowId()+" "+tempWorkflow.getWorkflowName()+" "+(maxFinishTime-tempWorkflow.getDeadline())+" Error: workflow's real finish time is larger than its deadline");
			}*/
		}
		
	}
	
	/**输出实验结果*/
	public void outputExperimentResult(List<SaaSVm> offVmList, List<Workflow> workflowList)
	{
		java.text.DecimalFormat fd = new java.text.DecimalFormat("0.0000"); //设定输出结果小数点的位数
		//统计调度指标：(1)费用；(2)资源利用率；(3)波动性
		double totalCost = 0;
		double totalExecutionTime = 0;
		double totalTime = 0;
		double deviation = 0;
		int taskNum = 0;
		
//		System.out.println("Vm Num: "+offVmList.size());
		for(SaaSVm offVm: offVmList)
		{
			totalCost = totalCost + offVm.getTotalCost();
						
			double workTime = 0;
			for(WTask task: offVm.getWTaskList())
			{
				workTime = workTime + task.getRealExecutionTime();							
				taskNum++; //任务的个数	
				deviation = deviation + (double)Math.abs((task.getRealStartTime()-task.getStartTimeWithConfidency()))/task.getRealExecutionTime();								
			}
			totalExecutionTime = totalExecutionTime + workTime;
			totalTime = totalTime + offVm.getEndWorkTime()-offVm.getVmStartWorkTime();
		}
		
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
//		System.out.println("WorkflowDeviation: "+fd.format(workflowDeviation));
		
		double reUtilization = (double)totalExecutionTime/totalTime;
		deviation = deviation/taskNum;
		//实验结果的输出	
		PerformanceValue.TotalCost = totalCost;
		PerformanceValue.ResourceUtilization = reUtilization;
		PerformanceValue.deviation = workflowDeviation;
		
		System.out.println("Total Cost: "+fd.format(totalCost)+" Resource Utilization: "+fd.format(reUtilization)+" Deviation: "+fd.format(workflowDeviation));		
	}//end outputExperimentResult(List<SaaSVm> offVmList)
	
	/**输出实验结果*/
/*	public void outputExperimentResult(List<SaaSVm> offVmList)
	{
		
		//统计调度指标：(1)费用；(2)资源利用率；(3)波动性
		double totalCost = 0;
		int totalExecutionTime = 0;
		int totalTime = 0;
		double deviation = 0;
		int taskNum = 0;
		
		for(SaaSVm offVm: offVmList)
		{
			totalCost = totalCost + offVm.getTotalCost();
						
			int workTime = 0;
			for(WTask task: offVm.getWTaskList())
			{
				workTime = workTime + task.getRealExecutionTime();							
				taskNum++; //任务的个数	
				
				deviation = deviation + (double)Math.abs((task.getRealStartTime()-task.getStartTimeWithConfidency()))/task.getRealExecutionTime();								
			}
			totalExecutionTime = totalExecutionTime + workTime;
			totalTime = totalTime + offVm.getEndWorkTime()-offVm.getVmStartWorkTime();
		}
		
		double reUtilization = (double)totalExecutionTime/totalTime;
		deviation = deviation/taskNum;
		//实验结果的输出	
		PerformanceValue.TotalCost = totalCost;
		PerformanceValue.ResourceUtilization = reUtilization;
		PerformanceValue.deviation = deviation;
		
		java.text.DecimalFormat fd = new java.text.DecimalFormat("0.0000"); //设定输出结果小数点的位数
		System.out.println("Total Cost: "+fd.format(totalCost)+" Resource Utilization: "+fd.format(reUtilization)+" Deviation: "+fd.format(deviation));		
	}//end outputExperimentResult(List<SaaSVm> offVmList) */
	
	/**更新工作流中任务与虚拟机的真正执行信息*/
	public void updateWTaskAndVmRealExecutionInformation(Workflow workflow)
	{
		List<WTask> calculatedTaskList = new ArrayList<WTask>(); //已经更新过的任务		
		while(true)
		{
			for(WTask lTask: workflow.getTaskList()) 
			{//对所有任务进行遍历一次
				if(lTask.getRealFinishTime() != -1)
				{//如果任务已经被更新，则跳过
					continue;
				}				
				
				
				double realStartTime = lTask.getAllocateVm().getRealFinishTime();
				if(realStartTime == -1)
				{
					realStartTime = lTask.getAllocateVm().getVmStartWorkTime();
				}
				
				boolean unCalculatedParent = false;
				if(lTask.getParentTaskList().size() != 0)
				{//确定任务的真正开始时间
					for(ConstraintWTask conWTask: lTask.getParentTaskList())
					{
						if(conWTask.getWTask().getRealFinishTime() != -1)
						{
							if(conWTask.getWTask().getRealFinishTime() > realStartTime)
							{//父任务的最大真正完成时间是该任务的真正真正执行时间
								realStartTime = conWTask.getWTask().getRealFinishTime();
							}
						}
						else
						{//该父任务还未被更新
							unCalculatedParent = true;
							break;
						}							
					}
				}
				if(unCalculatedParent)
				{//如果存在未更新的父任务，那么跳到其他任务
					continue;
				}
								
				//任务真正的执行时间
				double realExecutionTime = lTask.getRealBaseExecutionTime()*lTask.getAllocateVm().getVmFactor();
				//任务真正的完成时间
				double realFinishTime = realStartTime + realExecutionTime;
				//更新任务的状态
				lTask.setRealStartTime(realStartTime);
				lTask.setRealExecutionTime(realExecutionTime);
				lTask.setRealFinishTime(realFinishTime);				
				calculatedTaskList.add(lTask);
				//更新任务所在虚拟机的状态
				lTask.getAllocateVm().setRealFinishTime(realFinishTime);
				lTask.getAllocateVm().setReadyTime(realFinishTime);
				
/*				if(lTask.getTaskId().equals("ID00022") && lTask.getTaskWorkFlowId()==1)
				{
					System.out.println("Here");
				}*/
				
				//更新任务开始时间的分位点
				double startTimeWC = lTask.getStartTimeWithConfidency();
				for(ConstraintWTask conWTask: lTask.getParentTaskList())
				{
					if(conWTask.getWTask().getFinishTimeWithConfidency() > startTimeWC)
					{
						startTimeWC = conWTask.getWTask().getFinishTimeWithConfidency();
					}					
				}
				double finishTimeWC = startTimeWC + lTask.getBaseExecutionTime();
				lTask.setStartTimeWithConfidency(startTimeWC);
				lTask.setFinishTimeWithConfidency(finishTimeWC);
				
			}//end for(Task task: cWorkflow.getTaskList()) 
			
			if(calculatedTaskList.size() == workflow.getTaskList().size())
			{//所有任务都更新完的条件
				break;
			}
		}//end while
	}//end updateWTaskAndVmRealExecutionInformation()
	
	/**更新虚拟机的费用和资源消耗new*/
	public void updateActiveVmStatusNew(List<SaaSVm> activeVmList, List<SaaSVm> offVmList)
	{
		List<SaaSVm> turnOffVmList = new ArrayList<SaaSVm>(); //存放被关闭的虚拟机
		//将虚拟机按照真正结束时间进行升序排序
		Collections.sort(activeVmList, new SaaSVmComparatorByRealFinishTimeIncrease());
		for(SaaSVm activeVm: activeVmList)
		{
			double turnOffTime = activeVm.getRealFinishTime();
			//确定虚拟机关闭的时刻
			if((turnOffTime - activeVm.getVmStartWorkTime())%3600 != 0)
			{
				int round = (int)((turnOffTime - activeVm.getVmStartWorkTime())/3600);
				turnOffTime = activeVm.getVmStartWorkTime() + 3600*(round+1);
			}
			
			double workTime = turnOffTime - activeVm.getVmStartWorkTime();
			double cost = (workTime*activeVm.getVmPrice())/3600;
			
			activeVm.setEndWorkTime(turnOffTime);
			activeVm.setTotalCost(cost);
			activeVm.setVmStatus(false);
			turnOffVmList.add(activeVm); //将关闭的虚拟机加入到缓存队列中				
		}
		
		activeVmList.removeAll(turnOffVmList); //将关闭的虚拟机从活跃主机队列中移除
		offVmList.addAll(turnOffVmList); //将关闭的虚拟机加入到关闭的主机队列中
		
		if(activeVmList.size() != 0)
		{
			System.out.println("Error: there exists avtive vm in the system!");
		}	
	}//end updateActiveVmStatus()
	
	/**将系统中的活跃主机的状态更新到下一个任务到达的时刻*/
	public void updateActiveVmStatus(List<SaaSVm> activeVmList, List<SaaSVm> offVmList)
	{
		List<SaaSVm> turnOffVmList = new ArrayList<SaaSVm>(); //存放被关闭的虚拟机
		//将虚拟机按照真正结束时间进行升序排序
		Collections.sort(activeVmList, new SaaSVmComparatorByRealFinishTimeIncrease());
		for(SaaSVm activeVm: activeVmList)
		{
			double realFinishTime = 0;
			for(WTask lTask: activeVm.getWTaskList())
			{
				if(lTask.getRealFinishTime() > realFinishTime)
				{
					realFinishTime = lTask.getRealFinishTime();
				}
			}
			if(activeVm.getRealFinishTime() != realFinishTime)
			{
				System.out.println("Here");
			}
			
			if(activeVm.getRealFinishTime() <= StaticfinalTags.currentTime)
			{
				double turnOffTime = activeVm.getRealFinishTime();
				//确定虚拟机关闭的时刻
				if((turnOffTime - activeVm.getVmStartWorkTime())%3600 != 0)
				{
					int round = (int)((turnOffTime - activeVm.getVmStartWorkTime())/3600);
					turnOffTime = activeVm.getVmStartWorkTime() + 3600*(round+1);
				}
				
				if(turnOffTime <= StaticfinalTags.currentTime)
				{//对虚拟机activeVm进行关闭
					double workTime = turnOffTime - activeVm.getVmStartWorkTime();
					double cost = (workTime*activeVm.getVmPrice())/3600;
					
					activeVm.setEndWorkTime(turnOffTime);
					activeVm.setTotalCost(cost);
					activeVm.setVmStatus(false);
					
					double realExecutionTime = 0;
					for(WTask task: activeVm.getWTaskList())
					{						
						double executionTime = task.getRealFinishTime()-task.getRealStartTime();
						realExecutionTime += executionTime;
					}					
					turnOffVmList.add(activeVm); //将关闭的虚拟机加入到缓存队列中
				}
				else
				{//如果该虚拟机的关闭时刻在下一个工作流到达之后，那么跳到下一个虚拟机
					continue;
				}
										
				//需要在整点的时刻关闭
			}
			else
			{//如果虚拟机的完成时间大于下一个工作流到达的时刻，那么后面的虚拟机也不需要更新
				break;
			}				
		}
		
		activeVmList.removeAll(turnOffVmList); //将关闭的虚拟机从活跃主机队列中移除
		offVmList.addAll(turnOffVmList); //将关闭的虚拟机加入到关闭的主机队列中	
	}//end updateActiveVmStatus()
	
	/**将PCPs分配到虚拟机上*/
	public void allocatePcPsToSaaSVMs(Workflow allocateWorkflow, List<SaaSVm> acitveVMs, List<SaaSVm> offVMs)
	{	
		int num = 1;
		double maxFinishTime = allocateWorkflow.getDeadline(); //调度最大不能超过的时间
		while(true)
		{			
			List<WTask> selectedPcP = new ArrayList<WTask>(); //选出的PCP
			for(WTask task: allocateWorkflow.getTaskList())
			{
				if(task.getPCPNum() == num)
				{
					selectedPcP.add(task);
				}
			}
			if(selectedPcP.size() == 0)
			{//如果任务都调度完，则跳出while(true)
				break;
			}				
			num++;				
			
			Collections.sort(selectedPcP, new WTaskComparatorByBaseStartTimeIncrease());			
			double startTimeWithConfidency = allocateWorkflow.getArrivalTime(); //PCP的最早开始时间
			for(ConstraintWTask ConWTask: selectedPcP.get(0).getParentTaskList())
			{
				if(ConWTask.getWTask().getFinishTimeWithConfidency() > startTimeWithConfidency)
				{
					startTimeWithConfidency = ConWTask.getWTask().getFinishTimeWithConfidency();
				}
			}
			
			double baseExecutionTimeForPCP = 0; //PCP中所有任务的基准时间之和
			for(WTask tempTask: selectedPcP)
			{
				baseExecutionTimeForPCP = tempTask.getBaseExecutionTime();
			}
			
			double maxTempFinishTime = maxFinishTime;
			if(num != 2)
			{
				for(ConstraintWTask succTasks: selectedPcP.get(selectedPcP.size()-1).getSuccessorTaskList())
				{
					if(succTasks.getWTask().getAllocatedFlag() 
							&& succTasks.getWTask().getStartTimeWithConfidency()<maxTempFinishTime)
					{													
						maxTempFinishTime = succTasks.getWTask().getStartTimeWithConfidency();
					}
				}			
			}
						
			SaaSVm selectedVm = null;
			double minIdleTime = Double.MAX_VALUE;
			for(SaaSVm tempVm: acitveVMs)
			{//为PCP寻找在最晚完成时间内由最早完成时间的虚拟机				
				double tempStartTimeWF = startTimeWithConfidency; //可能的开始时间				
				if(tempVm.getReadyTime() > tempStartTimeWF)
				{
					tempStartTimeWF = tempVm.getReadyTime();
				}
				
				double tempStartTimeWithCon = tempStartTimeWF;
				double finishTimeWithConfidency = StaticfinalTags.currentTime;
				for(WTask tempTask: selectedPcP)
				{//对PCP中每一个任务进行测试
					if(tempStartTimeWithCon < (tempTask.getBaseStartTime()+allocateWorkflow.getArrivalTime()))
					{
						tempStartTimeWithCon = tempTask.getBaseStartTime()+allocateWorkflow.getArrivalTime();
					}					
					double tempFinishTime = tempStartTimeWithCon + tempTask.getBaseExecutionTime()*tempVm.getVmFactor();
					if(tempFinishTime > finishTimeWithConfidency)
					{
						finishTimeWithConfidency = tempFinishTime;
					}										
					tempStartTimeWithCon += tempTask.getBaseExecutionTime();
				}
				
				if(finishTimeWithConfidency <= maxTempFinishTime)
				{
					if((tempStartTimeWF - tempVm.getReadyTime()) < minIdleTime)
					{
						minIdleTime = (tempStartTimeWF - tempVm.getReadyTime());
						selectedVm = tempVm;
					}
				}
				
				if(maxTempFinishTime < startTimeWithConfidency)
				{//选出一台性能最好的虚拟机
					if(tempVm.getReadyTime()<tempStartTimeWF 
							&& tempVm.getVmType().equals("Extra-Large")
							&& (tempStartTimeWF - tempVm.getReadyTime()) < minIdleTime)
					{
						minIdleTime = (tempStartTimeWF - tempVm.getReadyTime());
						selectedVm = tempVm;
					}										
				}
			}
			
			if(selectedVm == null)
			{//增加虚拟机
				int vmId = acitveVMs.size() + offVMs.size(); //新虚拟机的ID
				//???确定一种虚拟机
				int level = determineSaaSVmType(startTimeWithConfidency, baseExecutionTimeForPCP, maxTempFinishTime);
				SaaSVm newVm = null;
				if(level == -1)
				{
					newVm = scaleUpVm(vmId, startTimeWithConfidency, 0);
				}
				else
				{
					newVm = scaleUpVm(vmId, startTimeWithConfidency, level);
				}				
//				SaaSVm newVm = scaleUpVm(vmId, startTimeWithConfidency, 0);
				acitveVMs.add(newVm);
				selectedVm = newVm;
			}
			
			if(selectedVm != null)
			{//将PCP中的所有任务分配到selectedVm上
				double startTimeWF = startTimeWithConfidency; //任务开始时间的分位点
				if(selectedVm.getWTaskList().size() != 0)
				{
					if(selectedVm.getReadyTime() > startTimeWF)
					{
						startTimeWF = selectedVm.getReadyTime();
					}					
				}
				
				int i = 0;				
				for(WTask allocatedTask: selectedPcP)
				{//将任务分配到虚拟机
					allocatedTask.setAllocatedFlag(true);
					allocatedTask.setAllocateVm(selectedVm);
					if(i==0)
					{//PCP中开始任务
						if(startTimeWF < (allocatedTask.getBaseStartTime()+allocateWorkflow.getArrivalTime()))
						{
							startTimeWF = allocatedTask.getBaseStartTime()+allocateWorkflow.getArrivalTime();
						}												
						allocatedTask.setStartTimeWithConfidency(startTimeWF);
						double finishTimeWithConfidency = startTimeWF + (int)(allocatedTask.getBaseExecutionTime()*selectedVm.getVmFactor());
						allocatedTask.setFinishTimeWithConfidency(finishTimeWithConfidency);
					}
					else
					{//PCP中非开始任务???有问题
						//????startTimeWC需要重新修改
						double startTimeWC = selectedPcP.get(i-1).getFinishTimeWithConfidency();
						if(startTimeWC < (allocatedTask.getBaseStartTime()+allocateWorkflow.getArrivalTime()))
						{
							startTimeWC = allocatedTask.getBaseStartTime()+allocateWorkflow.getArrivalTime();
						}
						
						double finishTimeWithConfidency = startTimeWC + allocatedTask.getBaseExecutionTime()*selectedVm.getVmFactor();
						allocatedTask.setStartTimeWithConfidency(startTimeWC);
						allocatedTask.setFinishTimeWithConfidency(finishTimeWithConfidency);
					}
					i++;
				}
				
				//更新虚拟机的状态
				selectedVm.getWTaskList().addAll(selectedPcP);
				double readyTime = selectedPcP.get(selectedPcP.size()-1).getFinishTimeWithConfidency();
				selectedVm.setReadyTime(readyTime);
			}//将PCP分配到虚拟机结束	
			
			if(num == 2)
			{
				double maxTime = StaticfinalTags.currentTime;
				for(WTask task: selectedPcP)
				{
					if(task.getFinishTimeWithConfidency() > maxTime)
					{
						maxTime = task.getFinishTimeWithConfidency();
					}
				}					
				maxFinishTime = maxTime;
			}
			
		}//end while(true)
	}//end allocatePcPsToSaaSVMs()
	
	public int determineSaaSVmType(double startTime, double executionTime, double maxFinishTime)
	{
		int level = -1;
		
		if(startTime + executionTime*1.6 <= maxFinishTime)
		{
			level = 4;
		}
		else if(startTime + executionTime*1.4 <= maxFinishTime)
		{
			level = 3;
		}
		else if(startTime + executionTime*1.2 <= maxFinishTime)
		{
			level = 2;
		}
		else if(startTime + executionTime*1.2 <= maxFinishTime)
		{
			level = 1;
		}
		else if(startTime + executionTime <= maxFinishTime)
		{
			level = 0;
		}
		
		return level;
	}
	
	/**计算工作流中每个任务的最晚结束、开始时间*/
	public void calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	{
		List<WTask> calculatedTaskList = new ArrayList<WTask>(); //已经计算过的任务		
		while(true)
		{
			for(WTask lTask: aWorkflow.getTaskList())//对所有任务进行遍历一次
			{
//				if(lTask.getLeastFinishTime() > -1)//如果任务已经计算过，则跳过
				if(lTask.getLeastFinishTime() != -1)//如果任务已经计算过，则跳过
				{
					continue;
				}
				
	//			double executionTime = lTask.getBaseExecutionTime();
				double executionTime = lTask.getExecutionTimeWithConfidency();
				if(lTask.getSuccessorTaskList().size() == 0)//计算结束任务的开始时间和结束时间
				{
					lTask.setLeastFinishTime(aWorkflow.getDeadline());
					lTask.setLeastStartTime(lTask.getLeastFinishTime()-executionTime);
					calculatedTaskList.add(lTask);
				}
				else//非结束任务,也就是有子任务的任务
				{					
					List<String> childTaskId = new ArrayList<String>();//存放所有父任务的Id
					for(Constraint con: lTask.getSuccessorIDList())
					{
						childTaskId.add(con.getTaskId());
					}//找出所有后继任务的Id					
					double minFinishTime = Integer.MAX_VALUE;
					boolean unCalculatedParent = false;
					for(String chidId: childTaskId) //判断每一个子任务是否已经计算过
					{
						for(WTask childTask: aWorkflow.getTaskList())
						{
							if(chidId.equals(childTask.getTaskId()))//找出子任务
							{
					//			if(childTask.getLeastFinishTime() > -1)
								if(childTask.getLeastFinishTime() != -1)
								{
//									if(childTask.getLeastFinishTime() < minFinishTime)
									if(childTask.getLeastStartTime() < minFinishTime)
									{
										minFinishTime = childTask.getLeastStartTime();
									}
								}
								else
								{
									unCalculatedParent = true;
								}
								break;
							}
						}
						if(unCalculatedParent==true)
						{
							break;
						}														
					}
					
					if(unCalculatedParent==false)//如果该任务所有父任务都完成了，那么计算它的开始和结束时间
					{
						lTask.setLeastFinishTime(minFinishTime);
						lTask.setLeastStartTime(lTask.getLeastFinishTime()-executionTime);
						calculatedTaskList.add(lTask);
					}						
				}//end else
			}//end for(Task task: cWorkflow.getTaskList()) 
			
			if(calculatedTaskList.get(calculatedTaskList.size()-1).getLeastFinishTime() < 0)
			{
				System.out.println("Error: the least finish time of WTask is less than zero!");
			}
			
			if(calculatedTaskList.size() == aWorkflow.getTaskList().size())//计算完的条件
			{
				break;
			}
		}//end while
	}//end calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	
	/**找出工作流中所有的PCPs*/
	public void searchPCPsForWorkflows(Workflow newWorkflow)
	{
		List<WTask> assignedWTaskList = new ArrayList<WTask>(); //已经加入到指定PCP的任务集合
		int PcPNum = 1;
		while(assignedWTaskList.size() != newWorkflow.getTaskList().size())
		{//直到所有的任务都分配完后，才退出
			WTask startWTask = null; //这一次寻找PCP的开始任务
			double maxBaseFinishTime = -1;
			for(WTask task: newWorkflow.getTaskList()) //从出口任务中寻找
			{
				if(task.getPCPNum() == -1 && task.getSuccessorTaskList().size() == 0) //没有后继任务的任务就是出口任务
				{//还未集类的出口任务
					if(task.getBaseFinishTime() > maxBaseFinishTime)
					{
						maxBaseFinishTime = task.getBaseFinishTime();
						startWTask = task;
					}						
				}
			}
			if(startWTask == null) //如果没有出口任务
			{
				for(WTask tempWTask: assignedWTaskList)
				{
					double tempMaxBaseFinishTime = -1;
					for(ConstraintWTask ConWTask: tempWTask.getParentTaskList())
					{
						if(ConWTask.getWTask().getPCPNum() == -1)
						{
							if(ConWTask.getWTask().getBaseFinishTime() > tempMaxBaseFinishTime)
							{
								tempMaxBaseFinishTime = ConWTask.getWTask().getBaseFinishTime();
								startWTask = ConWTask.getWTask();
							}
						}
							
					}
					if(startWTask != null)
					{
						break;
					}
				}
			}
			
/*			if(newWorkflow.getWorkflowId() == 1)
			{
				System.out.println("Here");
			}*/
			
			while(startWTask != null)
			{
				startWTask.setPCPNum(PcPNum);
				assignedWTaskList.add(startWTask);	
				WTask tempCPTask = null;
				double maxCPBaseFinishTime = -1;
				for(ConstraintWTask conWTask: startWTask.getParentTaskList())
				{
					
					if(conWTask.getWTask().getPCPNum() == -1 && conWTask.getWTask().getBaseFinishTime() > maxCPBaseFinishTime)
					{
						tempCPTask = conWTask.getWTask();
						maxCPBaseFinishTime = conWTask.getWTask().getBaseFinishTime();
					}
				}
				startWTask = tempCPTask;
			}
							
			PcPNum++; //PCP的标号
		}
		
/*		if(newWorkflow.getWorkflowId() == 1)
		{
			for(int index=1; index<PcPNum; index++)
			{
				System.out.println("PCP Num: "+index);
				for(WTask tempWTask: newWorkflow.getTaskList())
				{
					if(tempWTask.getPCPNum() == index)
					{
						System.out.print(tempWTask.getTaskId()+" ");
					}
				}
				System.out.println();
			}
			
		}
		
		System.out.println(); */
		
	}//end searchPCPsForWorkflows(Workflow newWorkflow)
			
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
	
	/**获取虚拟机*/
	public List<SaaSVm>  getVmList() {return vmList; }
	
	/**获取工作流*/
	public List<Workflow> getWorkflowList() {return workflowList; }
			
	/**提交工作流集合*/
	public void submitWorkflowList(List<Workflow> list)
	{
		getWorkflowList().addAll(list);
	}
	
	/**
	 * 根据虚拟机的类型产生虚拟机
	 * @param level对应不同类型的虚拟机，如0: Extra-Large; 1: High-CPU; 2: High-Memory; 3: Standard; 4: Micro;
	 * @return 新增的虚拟机
	 */
/*	public SaaSVm scaleUpVm(int vmId, int startTime, int level)
	{//????增加虚拟机的接口
		SaaSVm tempVm = null;
		switch(level)
		{
		case 0: //产生Extra-Large类型的虚拟机
			tempVm = new SaaSVm(vmId, "Extra-Large", startTime, 1.3, 1.0);
			break;
		case 1:
			tempVm = new SaaSVm(vmId, "High-CPU", startTime, 0.66, 1.2);
			break;
		case 2:
			tempVm = new SaaSVm(vmId, "High-Memory", startTime, 0.45, 1.2);
			break;
		case 3:
			tempVm = new SaaSVm(vmId, "Standard", startTime, 0.08, 1.4);
			break;
		case 4:
			tempVm = new SaaSVm(vmId, "Micro", startTime, 0.02, 1.6);
			break;		
		default:
			System.out.println("Warming: Only level= 0 1 2 3 4 5 are valid!");
		}		
		return tempVm;
	}*/
	
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
	
	/**根据任务的基准开始时间进行升序排序*/
	private class WTaskComparatorByBaseStartTimeIncrease implements Comparator<WTask>
	{
		public int compare(WTask task1, WTask task2)
		{
			if(task1.getBaseStartTime() > task2.getBaseStartTime())
			{
				return 1;
			}
			else if(task1.getBaseStartTime() < task2.getBaseStartTime())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
	
	/**根据虚拟机的真正完成时间进行升序排序*/
	private class SaaSVmComparatorByRealFinishTimeIncrease implements Comparator<SaaSVm>
	{
		public int compare(SaaSVm vm1, SaaSVm vm2)
		{
			if(vm1.getRealFinishTime() > vm2.getRealFinishTime())
			{
				return 1;
			}
			else if(vm1.getRealFinishTime() < vm2.getRealFinishTime())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
	
}
