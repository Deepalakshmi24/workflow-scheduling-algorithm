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

public class SHEFT_CompareAlgorithm 
{
	private List<SaaSVm> vmList; //虚拟机集合
	private List<Workflow> workflowList; //工作流集合
	
	public SHEFT_CompareAlgorithm() throws Exception
	{
		this.vmList = new ArrayList<SaaSVm>(); //只产生对象，里面为空
		initialVmList(StaticfinalTags.initialVmNum); //初始化系统中的虚拟机
		this.workflowList = new ArrayList<Workflow>(); //初始化工作流列表
	}
	
	/**算法SHEFT的实现，来自Xiaoyong Tang的文章A stochastic scheduling algorithm for precedence constrained tasks on Grid*/
	public void scheduleDynamicWorkflowsBySHEFT()
	{
		System.out.println("Algorithm SHEFT is Started");
		List<Workflow> workflowList = getWorkflowList();
		calculateTaskBaseExecutionTimeBeforeScheduling(workflowList); //计算出每个任务在调度时使用的时间
				
		int vmId = getVmList().size(); //全局虚拟机的ID
		List<SaaSVm> activeVmList = new ArrayList<SaaSVm>(); //系统中活跃虚拟机
		activeVmList = getVmList();		
		List<SaaSVm> offVmList = new ArrayList<SaaSVm>(); //系统中已经关闭的虚拟机
		
		//对每个任务进行调度
		for(int i=0; i<workflowList.size(); i++)
		{
			StaticfinalTags.currentTime = workflowList.get(i).getArrivalTime();
//			System.out.println("Workflow Id: "+workflowList.get(i).getWorkflowId());
			Workflow currentWorkflow = workflowList.get(i);
												
			//对工作流中的任务进行排序
			calculateWorkflowTaskLeastTime(currentWorkflow);			
			
			//将任务分配到虚拟机上
			scheduleWTaskToSaaSVm(currentWorkflow, activeVmList, vmId);			
			vmId = activeVmList.size() + offVmList.size(); //更新虚拟机的ID
			
			//更新系统当前的时间	
			if(i != workflowList.size()-1)
			{//非最后一个工作流，工作流分配后系统的状态更新到下一个任务到达的时刻	
				StaticfinalTags.currentTime = workflowList.get(i+1).getArrivalTime(); 				
			}
			else
			{//如果对最后一个工作流进行调度，那么将当前时间设置到最大，使得所有虚拟机的状态可以更新
				StaticfinalTags.currentTime = Integer.MAX_VALUE;
			}
			//对虚拟机状态进行更新(???需要检验活跃与关闭虚拟机是否已经分开)
			updateActiveVmStatus(activeVmList, offVmList);						
		}
		//对调度结果进行统计
		outputExperimentResult(offVmList, workflowList);
		
		verifyLogic(workflowList);
//		verifyWTaskOnSaaSVm(offVmList);
		
		workflowList.clear();
		offVmList.clear();
		activeVmList.clear();				
	}
	
	public void verifyLogic(List<Workflow> workflowList)
	{
		for(Workflow tempWorkflow: workflowList)
		{
			double maxFinishTimeWC = 0;
			double maxFinishTime = 0;
			for(WTask task: tempWorkflow.getTaskList())
			{
				if(task.getRealStartTime() < tempWorkflow.getArrivalTime())
				{
					System.out.println("Error: the task start time is less than workflow's arrival time ");
				}
				
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
		//			double comDelay = ConWTask.getDataSize()/StaticfinalTags.bandwidth;
					
					if(ConWTask.getWTask().getFinishTimeWithConfidency() > maxParentFinishTimeWC)
					{
						maxParentFinishTimeWC = ConWTask.getWTask().getFinishTimeWithConfidency();
					}
		//			if((ConWTask.getWTask().getRealFinishTime()+comDelay) > maxParentRealFinishTime)
					if((ConWTask.getWTask().getRealFinishTime()) > maxParentRealFinishTime)
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
					if((maxParentRealFinishTime-task.getRealStartTime()) > 2)
					{
						System.out.println("Interval: "+(maxParentRealFinishTime-task.getRealStartTime()));
					}
					
					System.out.println("Error: there exist errors in computing task real start time");
				}
			}
			
			if(maxFinishTimeWC > tempWorkflow.getDeadline())
			{
				System.out.println(tempWorkflow.getWorkflowId()+" "+tempWorkflow.getWorkflowName()+" "+(maxFinishTimeWC-tempWorkflow.getDeadline())+" Error: workflow's finish time with confidency is larger than its deadline");
			}
			if(maxFinishTime > tempWorkflow.getDeadline())
			{
				System.out.println(tempWorkflow.getWorkflowId()+" "+tempWorkflow.getWorkflowName()+" "+(maxFinishTime-tempWorkflow.getDeadline())+" Error: workflow's real finish time is larger than its deadline");
			}
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
		
		verifyWTaskOnSaaSVm(offVmList);
		
		for(SaaSVm offVm: offVmList)
		{
			totalCost = totalCost + offVm.getTotalCost();			
			
			double maxFinishTime = 0;
			
			double workTime = 0;
			for(WTask task: offVm.getWTaskList())
			{
				if(maxFinishTime > task.getRealFinishTime())
				{
					maxFinishTime = task.getRealFinishTime();
				}
				
				workTime = workTime + task.getRealExecutionTime();							
				taskNum++; //任务的个数	
				deviation = deviation + (double)Math.abs((task.getRealStartTime()-task.getStartTimeWithConfidency()))/task.getRealExecutionTime();								
			}
			totalExecutionTime = totalExecutionTime + workTime;
			totalTime = totalTime + offVm.getEndWorkTime()-offVm.getVmStartWorkTime();
			
/*			if((workTime/(offVm.getEndWorkTime()-offVm.getVmStartWorkTime())) >1)
			{
				System.out.println("RU for VM "+offVm.getVmID()+" is:"+fd.format((workTime/(offVm.getEndWorkTime()-offVm.getVmStartWorkTime())))+" Task Cost: "+workTime+" Vm Provision: "+(offVm.getEndWorkTime()-offVm.getVmStartWorkTime()));
			}*/
			
/*			if(offVm.getEndWorkTime() < maxFinishTime)
			{
				System.out.println("Error: task finish time is more than VM's off time!");
			}*/
				
							
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
		System.out.println("WorkflowDeviation: "+fd.format(workflowDeviation));
		
		double reUtilization = (double)totalExecutionTime/totalTime;
		deviation = deviation/taskNum;
		//实验结果的输出	
		PerformanceValue.TotalCost = totalCost;
		PerformanceValue.ResourceUtilization = reUtilization;
		PerformanceValue.deviation = workflowDeviation;
		
		System.out.println("Total Cost: "+fd.format(totalCost)+" Resource Utilization: "+fd.format(reUtilization)+" Deviation: "+fd.format(workflowDeviation));		
	}//end outputExperimentResult(List<SaaSVm> offVmList)
	
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
	
	/**将任务调度到虚拟机上*/
	public void scheduleWTaskToSaaSVm(Workflow workflow, List<SaaSVm> activeVmList, int vmID)
	{
		List<WTask> taskList = workflow.getTaskList();
		Collections.sort(taskList, new WTaskComparatorByLeastStartTimeIncrease());
		
		int vmId = vmID; //虚拟机的ID
		for(WTask task: taskList)
		{//对taskList中的每一个任务进行调度
			
/*			if(task.getTaskId().equals("ID00007") && task.getTaskWorkFlowId()==0)
			{
				System.out.println("Here!");
			}*/
			
			double minFinishTime = Integer.MAX_VALUE;
			SaaSVm selectedVm = null;
			double tempStartTimeWithConfidency = Integer.MAX_VALUE;
			for(SaaSVm tempVm: activeVmList)
			{//遍历所有的虚拟机
				//确定该任务task在虚拟机tempVm上的真正和期望开始时间
				double startTimeWithConfidency = tempVm.getReadyTime();
				if(workflow.getArrivalTime() > tempVm.getRealFinishTime())
				{
					startTimeWithConfidency = workflow.getArrivalTime();
				}
				
				//对前驱任务按照完成时间的分位数进行排序排序
				Collections.sort(task.getParentTaskList(), new PredWTaskComparatorByFinishTimeConfiIncrease());	
				for(ConstraintWTask parentCon: task.getParentTaskList())
				{//对任务的每个前驱任务进行检测
					SaaSVm parentTaskVm = parentCon.getWTask().getAllocateVm(); //前驱任务所在的虚拟机
					double minFinishTransTime = Double.MAX_VALUE;
					CommPort destinationPort = null;						
					
					//通信瓶颈的带宽
					double minBandwidth = tempVm.getCommPortList().get(0).getBandwidth();
					if(minBandwidth > StaticfinalTags.bandwidth)
					{
						minBandwidth = StaticfinalTags.bandwidth;
					}
					
					if(!parentTaskVm.equals(tempVm)) //前驱任务与当前任务不在同一虚拟机上的情况
					{//考虑数据传输: 与前驱任务不在同一虚拟机上的情况, 源主机已经关闭												
						for(CommPort port: tempVm.getCommPortList())
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
						CommRecord planRecord = new CommRecord(null, tempVm, parentCon.getDataSize(), 
								minBandwidth, startTransTime, minFinishTransTime);
						destinationPort.getPlanCommRecordList().add(planRecord);
					}//测试一个前驱任务结束
					else //前驱任务与当前任务在同一虚拟机上的情况
					{						
						minFinishTransTime = parentCon.getWTask().getFinishTimeWithConfidency();							
					}
															
					if(minFinishTransTime > startTimeWithConfidency)
					{//更新任务的开始时间
						startTimeWithConfidency = minFinishTransTime;														
					}
					
					if(!parentCon.getWTask().getAllocatedFlag())
					{//有前驱任务未计算过
						throw new IllegalArgumentException("There exists unfinished parent task!");
					}
				}//确定任务的最早开始时间				
				for(CommPort port: tempVm.getCommPortList())
				{//清理通信接口中预放置的通信
					port.getPlanCommRecordList().clear();
				}								
								
				double executionTimeWithConfidency = task.getExecutionTimeWithConfidency()*tempVm.getVmFactor();
				double finishTimeWithConfidency = startTimeWithConfidency + executionTimeWithConfidency;
				if(finishTimeWithConfidency <= task.getLeastFinishTime() && finishTimeWithConfidency < minFinishTime)
				{//找出有最小完成时间的虚拟机
					minFinishTime = finishTimeWithConfidency;
					selectedVm = tempVm;
					tempStartTimeWithConfidency = startTimeWithConfidency;
				}				
			}
			
			if(selectedVm != null)
			{//将任务task放置到虚拟机selectedVm上			
				//更新任务的状态
				double realStartTime = calculateRealStartTimeForWTask(task, selectedVm);				
				double realExecutionTime = task.getRealBaseExecutionTime()*selectedVm.getVmFactor();
				task.setAllocatedFlag(true);
				task.setAllocateVm(selectedVm);
				task.setRealStartTime(realStartTime);
				task.setStartTimeWithConfidency(tempStartTimeWithConfidency);
				task.setRealExecutionTime(realExecutionTime);
				task.setRealFinishTime(realStartTime + realExecutionTime);
				task.setFinishTimeWithConfidency(minFinishTime);
				
				//更新虚拟机的状态
				selectedVm.getWTaskList().add(task);
				selectedVm.setRealFinishTime(task.getRealFinishTime());
				selectedVm.setReadyTime(minFinishTime);	
			}
			else
			{//增加虚拟机来完成该任务
				SaaSVm newVm = addSaaSVmAndAllocateWTask(vmId, task);
				vmId++;
				activeVmList.add(newVm); //将新增加的虚拟机添加到活跃主机列表中
				
				//增加虚拟机
	/*			double newVmTime = workflow.getArrivalTime();
				for(ConstraintWTask parentCon: task.getParentTaskList())
				{//找出最大完成时间的父任务，以确定增加虚拟机的时刻					
					if(parentCon.getWTask().getFinishTimeWithConfidency() > newVmTime)
					{
						newVmTime = parentCon.getWTask().getFinishTimeWithConfidency();
					}
				}
								
				SaaSVm newVm = scaleUpVm(vmId, newVmTime, 0); //增加虚拟机
				vmId++;
				activeVmList.add(newVm); //将新增加的虚拟机添加到活跃主机列表中				
				
				//将任务分配到虚拟机上
				double executionTimeWithConfidency = task.getExecutionTimeWithConfidency()*newVm.getVmFactor();
				double realExecutionTime = task.getRealBaseExecutionTime()*newVm.getVmFactor();
				double finishTimeWithConfidency = newVmTime + executionTimeWithConfidency;
				double realFinishTime = newVmTime + realExecutionTime;
				
				//更新任务的状态
				task.setAllocatedFlag(true);
				task.setAllocateVm(newVm);
				task.setRealStartTime(newVmTime);
				task.setStartTimeWithConfidency(newVmTime);
				task.setRealExecutionTime(realExecutionTime);
				task.setRealFinishTime(realFinishTime);
				task.setFinishTimeWithConfidency(finishTimeWithConfidency);
								
				//更新虚拟机的状态
				newVm.getWTaskList().add(task);
				newVm.setRealFinishTime(task.getRealFinishTime());
				newVm.setReadyTime(finishTimeWithConfidency); */
			}									
		}//对每一个任务进行调度结束		
	}
	
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
		{
			throw new IllegalArgumentException("Error: can not add a new VM!");
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
		newVm.setRealFinishTime(realFinishtTime);
		newVm.setReadyTime(finishTimeWithConfidency);
		newVm.getWTaskList().add(task);
										
		return newVm;
	}
	
	
	/**确定任务放置在类型虚拟机上的最早开始时间new*/
	public double determineStartTimeForReadyTask(WTask task, int vmType)
	{	
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
		
	//	double startTime = 0; //???有问题
		double startTime = StaticfinalTags.currentTime;
		
		//对前驱任务按照完成时间的分位数进行排序排序
		Collections.sort(task.getParentTaskList(), new PredWTaskComparatorByFinishTimeConfiIncrease());		
		for(ConstraintWTask parentCon: task.getParentTaskList())
		{//对任务的每个前驱任务进行检测
			double minFinishTransTime = Double.MAX_VALUE;
			CommPort destinationPort = null;						
									
			for(CommPort port: commPortList)
			{//确定通信接口
				double startCommTime = parentCon.getWTask().getFinishTimeWithConfidency(); //开始通信时间								
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
	/*	if(task.getTaskId().equals("ID00000") && task.getTaskWorkFlowId()==0)
		{
			System.out.println("Here!");
		}*/
		
		double realStartTime = StaticfinalTags.currentTime;	
		if(realStartTime < targetVm.getRealFinishTime())
		{
			realStartTime = targetVm.getRealFinishTime();
		}
				
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
					double startCommTime = parentCon.getWTask().getRealFinishTime();					
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
						double maxEndTime = startCommTime;
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
				
				if(destinationPort == null)
				{
					System.out.println("Here!");
				}
				double startTransTime = minFinishTransTime - parentCon.getDataSize()/minBandwidth;
				CommRecord planRecord = new CommRecord(null, targetVm, parentCon.getDataSize(), 
						minBandwidth, startTransTime, minFinishTransTime);
				destinationPort.getPlanCommRecordList().add(planRecord);
			}//测试一个前驱任务结束
			else //前驱任务与当前任务在同一虚拟机上的情况
			{//不需要数据传输时间
				minFinishTransTime = parentCon.getWTask().getRealFinishTime();																	
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
		
	/*	if(realStartTime < targetVm.getVmStartWorkTime())
		{
			realStartTime = targetVm.getVmStartWorkTime();
		}*/				
		return realStartTime;
	}
	
	/**将系统中的活跃主机的状态更新到下一个任务到达的时刻*/
	public void updateActiveVmStatus(List<SaaSVm> activeVmList, List<SaaSVm> offVmList)
	{
		List<SaaSVm> turnOffVmList = new ArrayList<SaaSVm>(); //存放被关闭的虚拟机
		//将虚拟机按照真正结束时间进行升序排序
		Collections.sort(activeVmList, new SaaSVmComparatorByRealFinishTimeIncrease());
		for(SaaSVm activeVm: activeVmList)
		{
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
	}
	
	/**计算工作流中每个任务的最晚结束、开始时间*/
/*	public void calculateWorkflowTaskLeastTime(Workflow aWorkflow)
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
					List<String> childTaskId = new ArrayList<String>();//存放所有父任务的Id
					for(Constraint con: lTask.getSuccessorIDList())
					{
						childTaskId.add(con.getTaskId());
					}//找出所有后继任务的Id
					
					int minFinishTime = Integer.MAX_VALUE;
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
	}//end calculateWorkflowTaskLeastTime(Workflow aWorkflow) */
	
	/**计算工作流中每个任务的最晚结束、开始时间new*/
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
//						leastFinishTime = leastFinishTime - lTask.getBaseExecutionTime()*0.2;
						lTask.setLeastFinishTime(leastFinishTime);
						lTask.setLeastStartTime(leastFinishTime - executionTime);
						calculatedTaskList.add(lTask);
						if(lTask.getLeastStartTime() < aWorkflow.getArrivalTime())
						{
							throw new IllegalArgumentException("The least start time of task is less than its workflow's arrival time!");
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
/*	public SaaSVm scaleUpVm(int vmId, double startTime, int level)
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
//		System.out.println("Vm Type: "+level);
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
	
	
	/**根据任务的开始时间进行升序排序*/
	private class WTaskComparatorByLeastStartTimeIncrease implements Comparator<WTask>
	{
		public int compare(WTask cl1,WTask cl2)
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
	}//排序结束
}
