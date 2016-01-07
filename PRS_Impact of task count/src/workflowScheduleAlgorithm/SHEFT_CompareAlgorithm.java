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
	private List<SaaSVm> vmList; //���������
	private List<Workflow> workflowList; //����������
	
	public SHEFT_CompareAlgorithm() throws Exception
	{
		this.vmList = new ArrayList<SaaSVm>(); //ֻ������������Ϊ��
		initialVmList(StaticfinalTags.initialVmNum); //��ʼ��ϵͳ�е������
		this.workflowList = new ArrayList<Workflow>(); //��ʼ���������б�
	}
	
	/**�㷨SHEFT��ʵ�֣�����Xiaoyong Tang������A stochastic scheduling algorithm for precedence constrained tasks on Grid*/
	public void scheduleDynamicWorkflowsBySHEFT()
	{
		System.out.println("Algorithm SHEFT is Started");
		List<Workflow> workflowList = getWorkflowList();
		calculateTaskBaseExecutionTimeBeforeScheduling(workflowList); //�����ÿ�������ڵ���ʱʹ�õ�ʱ��
				
		int vmId = getVmList().size(); //ȫ���������ID
		List<SaaSVm> activeVmList = new ArrayList<SaaSVm>(); //ϵͳ�л�Ծ�����
		activeVmList = getVmList();		
		List<SaaSVm> offVmList = new ArrayList<SaaSVm>(); //ϵͳ���Ѿ��رյ������
		
		//��ÿ��������е���
		for(int i=0; i<workflowList.size(); i++)
		{
			StaticfinalTags.currentTime = workflowList.get(i).getArrivalTime();
//			System.out.println("Workflow Id: "+workflowList.get(i).getWorkflowId());
			Workflow currentWorkflow = workflowList.get(i);
												
			//�Թ������е������������
			calculateWorkflowTaskLeastTime(currentWorkflow);			
			
			//��������䵽�������
			scheduleWTaskToSaaSVm(currentWorkflow, activeVmList, vmId);			
			vmId = activeVmList.size() + offVmList.size(); //�����������ID
			
			//����ϵͳ��ǰ��ʱ��	
			if(i != workflowList.size()-1)
			{//�����һ���������������������ϵͳ��״̬���µ���һ�����񵽴��ʱ��	
				StaticfinalTags.currentTime = workflowList.get(i+1).getArrivalTime(); 				
			}
			else
			{//��������һ�����������е��ȣ���ô����ǰʱ�����õ����ʹ�������������״̬���Ը���
				StaticfinalTags.currentTime = Integer.MAX_VALUE;
			}
			//�������״̬���и���(???��Ҫ�����Ծ��ر�������Ƿ��Ѿ��ֿ�)
			updateActiveVmStatus(activeVmList, offVmList);						
		}
		//�Ե��Ƚ������ͳ��
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
	
	/**���ʵ����*/
	public void outputExperimentResult(List<SaaSVm> offVmList, List<Workflow> workflowList)
	{
		java.text.DecimalFormat fd = new java.text.DecimalFormat("0.0000"); //�趨������С�����λ��
		//ͳ�Ƶ���ָ�꣺(1)���ã�(2)��Դ�����ʣ�(3)������
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
				taskNum++; //����ĸ���	
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
		//ʵ���������	
		PerformanceValue.TotalCost = totalCost;
		PerformanceValue.ResourceUtilization = reUtilization;
		PerformanceValue.deviation = workflowDeviation;
		
		System.out.println("Total Cost: "+fd.format(totalCost)+" Resource Utilization: "+fd.format(reUtilization)+" Deviation: "+fd.format(workflowDeviation));		
	}//end outputExperimentResult(List<SaaSVm> offVmList)
	
	/**����������ϵ����������Ƿ����*/
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
	
	/**��������ȵ��������*/
	public void scheduleWTaskToSaaSVm(Workflow workflow, List<SaaSVm> activeVmList, int vmID)
	{
		List<WTask> taskList = workflow.getTaskList();
		Collections.sort(taskList, new WTaskComparatorByLeastStartTimeIncrease());
		
		int vmId = vmID; //�������ID
		for(WTask task: taskList)
		{//��taskList�е�ÿһ��������е���
			
/*			if(task.getTaskId().equals("ID00007") && task.getTaskWorkFlowId()==0)
			{
				System.out.println("Here!");
			}*/
			
			double minFinishTime = Integer.MAX_VALUE;
			SaaSVm selectedVm = null;
			double tempStartTimeWithConfidency = Integer.MAX_VALUE;
			for(SaaSVm tempVm: activeVmList)
			{//�������е������
				//ȷ��������task�������tempVm�ϵ�������������ʼʱ��
				double startTimeWithConfidency = tempVm.getReadyTime();
				if(workflow.getArrivalTime() > tempVm.getRealFinishTime())
				{
					startTimeWithConfidency = workflow.getArrivalTime();
				}
				
				//��ǰ�����������ʱ��ķ�λ��������������
				Collections.sort(task.getParentTaskList(), new PredWTaskComparatorByFinishTimeConfiIncrease());	
				for(ConstraintWTask parentCon: task.getParentTaskList())
				{//�������ÿ��ǰ��������м��
					SaaSVm parentTaskVm = parentCon.getWTask().getAllocateVm(); //ǰ���������ڵ������
					double minFinishTransTime = Double.MAX_VALUE;
					CommPort destinationPort = null;						
					
					//ͨ��ƿ���Ĵ���
					double minBandwidth = tempVm.getCommPortList().get(0).getBandwidth();
					if(minBandwidth > StaticfinalTags.bandwidth)
					{
						minBandwidth = StaticfinalTags.bandwidth;
					}
					
					if(!parentTaskVm.equals(tempVm)) //ǰ�������뵱ǰ������ͬһ������ϵ����
					{//�������ݴ���: ��ǰ��������ͬһ������ϵ����, Դ�����Ѿ��ر�												
						for(CommPort port: tempVm.getCommPortList())
						{//ȷ��ͨ�Žӿ�
							double startCommTime = StaticfinalTags.currentTime;
							if(parentCon.getWTask().getRealFinishTime() > startCommTime)
							{
								startCommTime = parentCon.getWTask().getFinishTimeWithConfidency();
							}
							
							if(port.getUnComCommRecordList().size() == 0 
									& port.getPlanCommRecordList().size() == 0)
							{//�ӿڿ��е����									
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
							{//�ӿ��л���ͨ�ŵ����
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
					}//����һ��ǰ���������
					else //ǰ�������뵱ǰ������ͬһ������ϵ����
					{						
						minFinishTransTime = parentCon.getWTask().getFinishTimeWithConfidency();							
					}
															
					if(minFinishTransTime > startTimeWithConfidency)
					{//��������Ŀ�ʼʱ��
						startTimeWithConfidency = minFinishTransTime;														
					}
					
					if(!parentCon.getWTask().getAllocatedFlag())
					{//��ǰ������δ�����
						throw new IllegalArgumentException("There exists unfinished parent task!");
					}
				}//ȷ����������翪ʼʱ��				
				for(CommPort port: tempVm.getCommPortList())
				{//����ͨ�Žӿ���Ԥ���õ�ͨ��
					port.getPlanCommRecordList().clear();
				}								
								
				double executionTimeWithConfidency = task.getExecutionTimeWithConfidency()*tempVm.getVmFactor();
				double finishTimeWithConfidency = startTimeWithConfidency + executionTimeWithConfidency;
				if(finishTimeWithConfidency <= task.getLeastFinishTime() && finishTimeWithConfidency < minFinishTime)
				{//�ҳ�����С���ʱ��������
					minFinishTime = finishTimeWithConfidency;
					selectedVm = tempVm;
					tempStartTimeWithConfidency = startTimeWithConfidency;
				}				
			}
			
			if(selectedVm != null)
			{//������task���õ������selectedVm��			
				//���������״̬
				double realStartTime = calculateRealStartTimeForWTask(task, selectedVm);				
				double realExecutionTime = task.getRealBaseExecutionTime()*selectedVm.getVmFactor();
				task.setAllocatedFlag(true);
				task.setAllocateVm(selectedVm);
				task.setRealStartTime(realStartTime);
				task.setStartTimeWithConfidency(tempStartTimeWithConfidency);
				task.setRealExecutionTime(realExecutionTime);
				task.setRealFinishTime(realStartTime + realExecutionTime);
				task.setFinishTimeWithConfidency(minFinishTime);
				
				//�����������״̬
				selectedVm.getWTaskList().add(task);
				selectedVm.setRealFinishTime(task.getRealFinishTime());
				selectedVm.setReadyTime(minFinishTime);	
			}
			else
			{//�������������ɸ�����
				SaaSVm newVm = addSaaSVmAndAllocateWTask(vmId, task);
				vmId++;
				activeVmList.add(newVm); //�������ӵ��������ӵ���Ծ�����б���
				
				//���������
	/*			double newVmTime = workflow.getArrivalTime();
				for(ConstraintWTask parentCon: task.getParentTaskList())
				{//�ҳ�������ʱ��ĸ�������ȷ�������������ʱ��					
					if(parentCon.getWTask().getFinishTimeWithConfidency() > newVmTime)
					{
						newVmTime = parentCon.getWTask().getFinishTimeWithConfidency();
					}
				}
								
				SaaSVm newVm = scaleUpVm(vmId, newVmTime, 0); //���������
				vmId++;
				activeVmList.add(newVm); //�������ӵ��������ӵ���Ծ�����б���				
				
				//��������䵽�������
				double executionTimeWithConfidency = task.getExecutionTimeWithConfidency()*newVm.getVmFactor();
				double realExecutionTime = task.getRealBaseExecutionTime()*newVm.getVmFactor();
				double finishTimeWithConfidency = newVmTime + executionTimeWithConfidency;
				double realFinishTime = newVmTime + realExecutionTime;
				
				//���������״̬
				task.setAllocatedFlag(true);
				task.setAllocateVm(newVm);
				task.setRealStartTime(newVmTime);
				task.setStartTimeWithConfidency(newVmTime);
				task.setRealExecutionTime(realExecutionTime);
				task.setRealFinishTime(realFinishTime);
				task.setFinishTimeWithConfidency(finishTimeWithConfidency);
								
				//�����������״̬
				newVm.getWTaskList().add(task);
				newVm.setRealFinishTime(task.getRealFinishTime());
				newVm.setReadyTime(finishTimeWithConfidency); */
			}									
		}//��ÿһ��������е��Ƚ���		
	}
	
	/**���������������������new*/
	public SaaSVm addSaaSVmAndAllocateWTask(int vmID, WTask task)
	{		
		SaaSVm newVm = null;	
		double startTime04 = determineStartTimeForReadyTask(task, 4); //�������ݴ������������翪ʼʱ��				
		double startTime03 = determineStartTimeForReadyTask(task, 3);
		double startTime02 = determineStartTimeForReadyTask(task, 2);
		double startTime01 = determineStartTimeForReadyTask(task, 1);
		double startTime00 = determineStartTimeForReadyTask(task, 0);
		
		double startTime = StaticfinalTags.currentTime; //Ԥ�ƵĿ�ʼʱ��
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
		
		//��������scheduleTask��״̬					
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
							
		//���������vm��״̬
		newVm.setRealFinishTime(realFinishtTime);
		newVm.setReadyTime(finishTimeWithConfidency);
		newVm.getWTaskList().add(task);
										
		return newVm;
	}
	
	
	/**ȷ���������������������ϵ����翪ʼʱ��new*/
	public double determineStartTimeForReadyTask(WTask task, int vmType)
	{	
		double minBandwidth = 0; //ͨ��ƿ���Ĵ���
		switch(vmType)
		{
		case 0: //����Extra-Large���͵������
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
		{//�����������ͨ�Žӿ�
			CommPort port = new CommPort(i, minBandwidth);
			commPortList.add(port);
		}
		
	//	double startTime = 0; //???������
		double startTime = StaticfinalTags.currentTime;
		
		//��ǰ�����������ʱ��ķ�λ��������������
		Collections.sort(task.getParentTaskList(), new PredWTaskComparatorByFinishTimeConfiIncrease());		
		for(ConstraintWTask parentCon: task.getParentTaskList())
		{//�������ÿ��ǰ��������м��
			double minFinishTransTime = Double.MAX_VALUE;
			CommPort destinationPort = null;						
									
			for(CommPort port: commPortList)
			{//ȷ��ͨ�Žӿ�
				double startCommTime = parentCon.getWTask().getFinishTimeWithConfidency(); //��ʼͨ��ʱ��								
				if(port.getPlanCommRecordList().size() == 0)
				{//�ӿڿ��е����									
					minFinishTransTime = startCommTime + parentCon.getDataSize()/minBandwidth;
					destinationPort = port;
					break;
				}
				else if(port.getPlanCommRecordList().size() != 0)
				{//�ӿ��л���ͨ�ŵ����
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
			{//��������Ŀ�ʼʱ��
				startTime = minFinishTransTime;														
			}
			
			if(!parentCon.getWTask().getAllocatedFlag())
			{//��ǰ������δ�����
				throw new IllegalArgumentException("There exists unfinished parent task!");
			}
		}
		
		return startTime;
	}
			
	/**��ȡ���������������ϵ�������ʼʱ��new*/
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
				
		//��ǰ���������������ʱ�������������
		Collections.sort(task.getParentTaskList(), new PredWTaskComparatorByRealFinishTimeIncrease());
		for(ConstraintWTask parentCon: task.getParentTaskList())
		{//�������ÿ��ǰ��������м��
			SaaSVm parentTaskVm = parentCon.getWTask().getAllocateVm(); //ǰ���������ڵ������
			double minFinishTransTime = Double.MAX_VALUE;
			CommPort destinationPort = null;						
			
			//ͨ��ƿ���Ĵ���
			double minBandwidth = targetVm.getCommPortList().get(0).getBandwidth();
			if(minBandwidth > StaticfinalTags.bandwidth)
			{
				minBandwidth = StaticfinalTags.bandwidth;
			}
			
			if(!parentTaskVm.equals(targetVm)) //ǰ�������뵱ǰ������ͬһ������ϵ����
			{//�������ݴ���: ��ǰ��������ͬһ������ϵ����, Դ�����Ѿ��ر�								
				for(CommPort port: targetVm.getCommPortList())
				{//ȷ��ͨ�Žӿ�
					double startCommTime = parentCon.getWTask().getRealFinishTime();					
					if(port.getUnComCommRecordList().size() == 0 
							& port.getPlanCommRecordList().size() == 0)
					{//�ӿڿ��е����									
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
					{//�ӿ��л���ͨ�ŵ����
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
			}//����һ��ǰ���������
			else //ǰ�������뵱ǰ������ͬһ������ϵ����
			{//����Ҫ���ݴ���ʱ��
				minFinishTransTime = parentCon.getWTask().getRealFinishTime();																	
			}
													
			if(minFinishTransTime > realStartTime)
			{//��������Ŀ�ʼʱ��
				realStartTime = minFinishTransTime;														
			}
		}//ȷ����������翪ʼʱ��				
		for(CommPort port: targetVm.getCommPortList())
		{//����ͨ�Žӿ���Ԥ���õ�ͨ��
			port.getPlanCommRecordList().clear();
		}
		
	/*	if(realStartTime < targetVm.getVmStartWorkTime())
		{
			realStartTime = targetVm.getVmStartWorkTime();
		}*/				
		return realStartTime;
	}
	
	/**��ϵͳ�еĻ�Ծ������״̬���µ���һ�����񵽴��ʱ��*/
	public void updateActiveVmStatus(List<SaaSVm> activeVmList, List<SaaSVm> offVmList)
	{
		List<SaaSVm> turnOffVmList = new ArrayList<SaaSVm>(); //��ű��رյ������
		//�������������������ʱ�������������
		Collections.sort(activeVmList, new SaaSVmComparatorByRealFinishTimeIncrease());
		for(SaaSVm activeVm: activeVmList)
		{
			if(activeVm.getRealFinishTime() <= StaticfinalTags.currentTime)
			{
				double turnOffTime = activeVm.getRealFinishTime();
				//ȷ��������رյ�ʱ��
				if((turnOffTime - activeVm.getVmStartWorkTime())%3600 != 0)
				{
					int round = (int)((turnOffTime - activeVm.getVmStartWorkTime())/3600);
					turnOffTime = activeVm.getVmStartWorkTime() + 3600*(round+1);
				}
				
				if(turnOffTime <= StaticfinalTags.currentTime)
				{//�������activeVm���йر�
					double workTime = turnOffTime - activeVm.getVmStartWorkTime();
					double cost = (workTime*activeVm.getVmPrice())/3600;
					
					activeVm.setEndWorkTime(turnOffTime);
					activeVm.setTotalCost(cost);
					activeVm.setVmStatus(false);
					
					turnOffVmList.add(activeVm); //���رյ���������뵽���������
				}
				else
				{//�����������Ĺر�ʱ������һ������������֮����ô������һ�������
					continue;
				}
										
				//��Ҫ�������ʱ�̹ر�
			}
			else
			{//�������������ʱ�������һ�������������ʱ�̣���ô����������Ҳ����Ҫ����
				break;
			}				
		}
		
		activeVmList.removeAll(turnOffVmList); //���رյ�������ӻ�Ծ�����������Ƴ�
		offVmList.addAll(turnOffVmList); //���رյ���������뵽�رյ�����������
	}
	
	/**���㹤������ÿ������������������ʼʱ��*/
/*	public void calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	{
		List<WTask> calculatedTaskList = new ArrayList<WTask>(); //�Ѿ������������		
		while(true)
		{
			for(WTask lTask: aWorkflow.getTaskList())//������������б���һ��
			{
				if(lTask.getLeastFinishTime() != -1)//��������Ѿ��������������
				{
					continue;
				}
				
				double executionTime = lTask.getExecutionTimeWithConfidency();
				if(lTask.getSuccessorTaskList().size() == 0)//�����������Ŀ�ʼʱ��ͽ���ʱ��
				{
					lTask.setLeastFinishTime(aWorkflow.getDeadline());
					lTask.setLeastStartTime(lTask.getLeastFinishTime()-executionTime);
					calculatedTaskList.add(lTask);
				}
				else//�ǽ�������,Ҳ�����������������
				{					
					List<String> childTaskId = new ArrayList<String>();//������и������Id
					for(Constraint con: lTask.getSuccessorIDList())
					{
						childTaskId.add(con.getTaskId());
					}//�ҳ����к�������Id
					
					int minFinishTime = Integer.MAX_VALUE;
					boolean unCalculatedParent = false;
					for(String chidId: childTaskId) //�ж�ÿһ���������Ƿ��Ѿ������
					{
						for(WTask childTask: aWorkflow.getTaskList())
						{
							if(chidId.equals(childTask.getTaskId()))//�ҳ�������
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
					
					if(unCalculatedParent==false)//������������и���������ˣ���ô�������Ŀ�ʼ�ͽ���ʱ��
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
			
			if(calculatedTaskList.size() == aWorkflow.getTaskList().size())//�����������
			{
				break;
			}
		}//end while
	}//end calculateWorkflowTaskLeastTime(Workflow aWorkflow) */
	
	/**���㹤������ÿ������������������ʼʱ��new*/
	public void calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	{
		List<WTask> calculatedTaskList = new ArrayList<WTask>(); //�Ѿ������������		
		while(true)
		{
			for(WTask lTask: aWorkflow.getTaskList())//������������б���һ��
			{
				if(lTask.getLeastFinishTime() != -1)//��������Ѿ��������������
				{
					continue;
				}
				
				double executionTime = lTask.getExecutionTimeWithConfidency();
				if(lTask.getSuccessorTaskList().size() == 0)//�����������Ŀ�ʼʱ��ͽ���ʱ��
				{
					lTask.setLeastFinishTime(aWorkflow.getDeadline());
					lTask.setLeastStartTime(lTask.getLeastFinishTime()-executionTime);
					calculatedTaskList.add(lTask);
				}
				else//�ǽ�������,Ҳ�����������������
				{	
					double leastFinishTime = Double.MAX_VALUE; //����ʼʱ��
					boolean unCalculatedSucessor = false; //�Ƿ����δ�����������
					for(ConstraintWTask sucessorCon: lTask.getSuccessorTaskList())
					{//�������ÿ������������м��
						//һ���ӿ�ͨ���ǿ��ܵ����ͨ������
						int round = (sucessorCon.getWTask().getParentTaskList().size()/StaticfinalTags.portNum+2);
						double maxDataSize = 0;
						for(ConstraintWTask predCon: sucessorCon.getWTask().getParentTaskList())
						{
							if(predCon.getDataSize() > maxDataSize)
							{
								maxDataSize = predCon.getDataSize();
							}
						}
						//ͨ���ӳ�
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
						{//�к�������δ�����
							unCalculatedSucessor = true;
							break;
						}
					}
					if(unCalculatedSucessor == false)
					{//�����������������������ˣ���ô������������ʼ���������ʱ��
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
			
			if(calculatedTaskList.size() == aWorkflow.getTaskList().size())//�����������
			{
				break;
			}
		}//end while
	}//end calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	
	/**���������ڵ���ǰʹ�õĻ�׼ִ��ʱ�䣺�����ƽ��ִ��ʱ����ϱ�׼��*/
	public static void calculateTaskBaseExecutionTimeBeforeScheduling(List<Workflow> list)
	{
		for(Workflow tempWorkflow: list)
		{//��ÿ�����������б���
			for(WTask tempTask: tempWorkflow.getTaskList())
			{//��ÿ���������е�ÿ��������б���
				double standardDeviation = tempTask.getBaseExecutionTime()*StaticfinalTags.standardDeviation;				
				//�������ǰʹ�õ�ִ��ʱ�������ִ��ʱ���ƽ��ֵ����ִ��ʱ��ı�׼��
				double executionTimeWithConfidency =  tempTask.getBaseExecutionTime() + standardDeviation;
				tempTask.setExecutionTimeWithConfidency(executionTimeWithConfidency);
			}
		}
	}
		
	/**��ʼ��ϵͳ�е������: ÿ���������������������ͬ*/
	public void initialVmList(int num)
	{
		List<SaaSVm> initialVms = new ArrayList<SaaSVm>(); //��ʼ��
		for(int i=0; i<num; i++)
		{
			int level = i%5;
			SaaSVm initialVm = scaleUpVm(i, 0, level);
			initialVms.add(initialVm);
		}
		getVmList().addAll(initialVms);		
	}
	
	/**��ȡ�����*/
	public List<SaaSVm>  getVmList() {return vmList; }
	
	/**��ȡ������*/
	public List<Workflow> getWorkflowList() {return workflowList; }
			
	/**�ύ����������*/
	public void submitWorkflowList(List<Workflow> list)
	{
		getWorkflowList().addAll(list);
	}
	
	/**
	 * ��������������Ͳ��������
	 * @param level��Ӧ��ͬ���͵����������0: Extra-Large; 1: High-CPU; 2: High-Memory; 3: Standard; 4: Micro;
	 * @return �����������
	 */
/*	public SaaSVm scaleUpVm(int vmId, double startTime, int level)
	{//????����������Ľӿ�
		SaaSVm tempVm = null;
		switch(level)
		{
		case 0: //����Extra-Large���͵������
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
	 * ��������������Ͳ��������
	 * @param level��Ӧ��ͬ���͵����������0: Extra-Large; 1: High-CPU; 2: High-Memory; 3: Standard; 4: Micro;
	 * @return �����������
	 */
	public SaaSVm scaleUpVm(int vmId, double startTime, int level)
	{
//		System.out.println("Vm Type: "+level);
		SaaSVm tempVm = null;
		switch(level)
		{
		case 0: //����Extra-Large���͵������
			List<CommPort> commPortList0 = new ArrayList<CommPort>();
			for(int i=0; i<StaticfinalTags.portNum; i++) //6GB
			{//�����������ͨ�Žӿ�
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
			{//�����������ͨ�Žӿ�
				CommPort port = new CommPort(i, 1000000);
				commPortList1.add(port);
			}			
			tempVm = new SaaSVm(vmId, "High-CPU", startTime, 0.66, 1.2, commPortList1);
			break;
		case 2:
			List<CommPort> commPortList2 = new ArrayList<CommPort>();
			for(int i=0; i<StaticfinalTags.portNum; i++) //4GB
			{//�����������ͨ�Žӿ�
				CommPort port = new CommPort(i, 800000);
				commPortList2.add(port);
			}			
			tempVm = new SaaSVm(vmId, "High-Memory", startTime, 0.45, 1.2, commPortList2);
			break;
		case 3: 
			List<CommPort> commPortList3 = new ArrayList<CommPort>();
			for(int i=0; i<StaticfinalTags.portNum; i++) //3GB
			{//�����������ͨ�Žӿ�
				CommPort port = new CommPort(i, 600000);
				commPortList3.add(port);
			}
			tempVm = new SaaSVm(vmId, "Standard", startTime, 0.08, 1.4, commPortList3);	
			break;
		case 4:
			List<CommPort> commPortList4 = new ArrayList<CommPort>();
			for(int i=0; i<StaticfinalTags.portNum; i++) //2GB
			{//�����������ͨ�Žӿ�
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
	
	/**����ǰ��������������ʱ�������������*/
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
	
	/**����ǰ���������ʱ��ķ�λ�������������*/
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
	
	
	/**��������Ŀ�ʼʱ�������������*/
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
	
	/**��������������ʼʱ�������������*/
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
	
	/**������������������ʱ�������������*/
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
	}//�������
}
