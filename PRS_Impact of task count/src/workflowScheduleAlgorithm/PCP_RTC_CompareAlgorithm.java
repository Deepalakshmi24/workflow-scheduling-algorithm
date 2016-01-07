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
	private List<SaaSVm> vmList; //���������
	private List<Workflow> workflowList; //����������
	
	public PCP_RTC_CompareAlgorithm() throws Exception
	{
		this.vmList = new ArrayList<SaaSVm>(); //ֻ������������Ϊ��
		initialVmList(StaticfinalTags.initialVmNum); //��ʼ��ϵͳ�е������
		this.workflowList = new ArrayList<Workflow>(); //��ʼ���������б�
	}
	
	/**�㷨RTC��ʵ�֣�����Deepak Poola������Robust Scheduling of Scientific Workflows with Deadline and Budget Constraints in Clouds*/
	public void scheduleDynamicWorkflowsByRTC()
	{
		System.out.println("Algorithm RTC is Started");
		List<Workflow> workflowList = getWorkflowList();
		calculateTaskBaseExecutionTimeBeforeScheduling(workflowList);
		
//		int vmId = getVmList().size(); //ȫ���������ID
		List<SaaSVm> activeVmList = new ArrayList<SaaSVm>(); //ϵͳ�л�Ծ�����
		activeVmList = getVmList();		
		List<SaaSVm> offVmList = new ArrayList<SaaSVm>(); //ϵͳ���Ѿ��رյ������
		
		//��ÿ��������е���
		for(int i=0; i<workflowList.size(); i++)
		{
	//		System.out.println("Start WorkflowID: "+i);			
			StaticfinalTags.currentTime = workflowList.get(i).getArrivalTime(); 
			//���㹤�������������������ʼ������ʱ��
			calculateWorkflowTaskLeastTime(workflowList.get(i));			
			//�ҳ����������е�PCPs
			searchPCPsForWorkflows(workflowList.get(i));						
			//�����е�PCPs���䵽�������
			allocatePcPsToSaaSVMs(workflowList.get(i), activeVmList, offVmList);			
			
			if(i != workflowList.size()-1)
			{//�����һ���������������������ϵͳ��״̬���µ���һ�����񵽴��ʱ��	
				StaticfinalTags.currentTime = workflowList.get(i+1).getArrivalTime(); 				
			}
			else
			{//��������һ�����������е��ȣ���ô����ǰʱ�����õ����ʹ�������������״̬���Ը���
				StaticfinalTags.currentTime = Integer.MAX_VALUE;
			}
			//����ÿ�����������������ִ�п�ʼʱ��
			updateWTaskAndVmRealExecutionInformation(workflowList.get(i));
			//�����������״̬			
			updateActiveVmStatus(activeVmList, offVmList);
//			updateActiveVmStatusNew(activeVmList, offVmList);
		}		
		//ͳ��ʵ����
		outputExperimentResult(offVmList, workflowList);	
		
		//��������ķ���˳���Ƿ���ȷ
//		verifyLogic(workflowList); 
		
		workflowList.clear();
		offVmList.clear();
		activeVmList.clear();				
	}
	
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
		
//		System.out.println("Vm Num: "+offVmList.size());
		for(SaaSVm offVm: offVmList)
		{
			totalCost = totalCost + offVm.getTotalCost();
						
			double workTime = 0;
			for(WTask task: offVm.getWTaskList())
			{
				workTime = workTime + task.getRealExecutionTime();							
				taskNum++; //����ĸ���	
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
		//ʵ���������	
		PerformanceValue.TotalCost = totalCost;
		PerformanceValue.ResourceUtilization = reUtilization;
		PerformanceValue.deviation = workflowDeviation;
		
		System.out.println("Total Cost: "+fd.format(totalCost)+" Resource Utilization: "+fd.format(reUtilization)+" Deviation: "+fd.format(workflowDeviation));		
	}//end outputExperimentResult(List<SaaSVm> offVmList)
	
	/**���ʵ����*/
/*	public void outputExperimentResult(List<SaaSVm> offVmList)
	{
		
		//ͳ�Ƶ���ָ�꣺(1)���ã�(2)��Դ�����ʣ�(3)������
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
				taskNum++; //����ĸ���	
				
				deviation = deviation + (double)Math.abs((task.getRealStartTime()-task.getStartTimeWithConfidency()))/task.getRealExecutionTime();								
			}
			totalExecutionTime = totalExecutionTime + workTime;
			totalTime = totalTime + offVm.getEndWorkTime()-offVm.getVmStartWorkTime();
		}
		
		double reUtilization = (double)totalExecutionTime/totalTime;
		deviation = deviation/taskNum;
		//ʵ���������	
		PerformanceValue.TotalCost = totalCost;
		PerformanceValue.ResourceUtilization = reUtilization;
		PerformanceValue.deviation = deviation;
		
		java.text.DecimalFormat fd = new java.text.DecimalFormat("0.0000"); //�趨������С�����λ��
		System.out.println("Total Cost: "+fd.format(totalCost)+" Resource Utilization: "+fd.format(reUtilization)+" Deviation: "+fd.format(deviation));		
	}//end outputExperimentResult(List<SaaSVm> offVmList) */
	
	/**���¹������������������������ִ����Ϣ*/
	public void updateWTaskAndVmRealExecutionInformation(Workflow workflow)
	{
		List<WTask> calculatedTaskList = new ArrayList<WTask>(); //�Ѿ����¹�������		
		while(true)
		{
			for(WTask lTask: workflow.getTaskList()) 
			{//������������б���һ��
				if(lTask.getRealFinishTime() != -1)
				{//��������Ѿ������£�������
					continue;
				}				
				
				
				double realStartTime = lTask.getAllocateVm().getRealFinishTime();
				if(realStartTime == -1)
				{
					realStartTime = lTask.getAllocateVm().getVmStartWorkTime();
				}
				
				boolean unCalculatedParent = false;
				if(lTask.getParentTaskList().size() != 0)
				{//ȷ�������������ʼʱ��
					for(ConstraintWTask conWTask: lTask.getParentTaskList())
					{
						if(conWTask.getWTask().getRealFinishTime() != -1)
						{
							if(conWTask.getWTask().getRealFinishTime() > realStartTime)
							{//�����������������ʱ���Ǹ��������������ִ��ʱ��
								realStartTime = conWTask.getWTask().getRealFinishTime();
							}
						}
						else
						{//�ø�����δ������
							unCalculatedParent = true;
							break;
						}							
					}
				}
				if(unCalculatedParent)
				{//�������δ���µĸ�������ô������������
					continue;
				}
								
				//����������ִ��ʱ��
				double realExecutionTime = lTask.getRealBaseExecutionTime()*lTask.getAllocateVm().getVmFactor();
				//�������������ʱ��
				double realFinishTime = realStartTime + realExecutionTime;
				//���������״̬
				lTask.setRealStartTime(realStartTime);
				lTask.setRealExecutionTime(realExecutionTime);
				lTask.setRealFinishTime(realFinishTime);				
				calculatedTaskList.add(lTask);
				//�������������������״̬
				lTask.getAllocateVm().setRealFinishTime(realFinishTime);
				lTask.getAllocateVm().setReadyTime(realFinishTime);
				
/*				if(lTask.getTaskId().equals("ID00022") && lTask.getTaskWorkFlowId()==1)
				{
					System.out.println("Here");
				}*/
				
				//��������ʼʱ��ķ�λ��
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
			{//�������񶼸����������
				break;
			}
		}//end while
	}//end updateWTaskAndVmRealExecutionInformation()
	
	/**����������ķ��ú���Դ����new*/
	public void updateActiveVmStatusNew(List<SaaSVm> activeVmList, List<SaaSVm> offVmList)
	{
		List<SaaSVm> turnOffVmList = new ArrayList<SaaSVm>(); //��ű��رյ������
		//�������������������ʱ�������������
		Collections.sort(activeVmList, new SaaSVmComparatorByRealFinishTimeIncrease());
		for(SaaSVm activeVm: activeVmList)
		{
			double turnOffTime = activeVm.getRealFinishTime();
			//ȷ��������رյ�ʱ��
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
			turnOffVmList.add(activeVm); //���رյ���������뵽���������				
		}
		
		activeVmList.removeAll(turnOffVmList); //���رյ�������ӻ�Ծ�����������Ƴ�
		offVmList.addAll(turnOffVmList); //���رյ���������뵽�رյ�����������
		
		if(activeVmList.size() != 0)
		{
			System.out.println("Error: there exists avtive vm in the system!");
		}	
	}//end updateActiveVmStatus()
	
	/**��ϵͳ�еĻ�Ծ������״̬���µ���һ�����񵽴��ʱ��*/
	public void updateActiveVmStatus(List<SaaSVm> activeVmList, List<SaaSVm> offVmList)
	{
		List<SaaSVm> turnOffVmList = new ArrayList<SaaSVm>(); //��ű��رյ������
		//�������������������ʱ�������������
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
					
					double realExecutionTime = 0;
					for(WTask task: activeVm.getWTaskList())
					{						
						double executionTime = task.getRealFinishTime()-task.getRealStartTime();
						realExecutionTime += executionTime;
					}					
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
	}//end updateActiveVmStatus()
	
	/**��PCPs���䵽�������*/
	public void allocatePcPsToSaaSVMs(Workflow allocateWorkflow, List<SaaSVm> acitveVMs, List<SaaSVm> offVMs)
	{	
		int num = 1;
		double maxFinishTime = allocateWorkflow.getDeadline(); //��������ܳ�����ʱ��
		while(true)
		{			
			List<WTask> selectedPcP = new ArrayList<WTask>(); //ѡ����PCP
			for(WTask task: allocateWorkflow.getTaskList())
			{
				if(task.getPCPNum() == num)
				{
					selectedPcP.add(task);
				}
			}
			if(selectedPcP.size() == 0)
			{//������񶼵����꣬������while(true)
				break;
			}				
			num++;				
			
			Collections.sort(selectedPcP, new WTaskComparatorByBaseStartTimeIncrease());			
			double startTimeWithConfidency = allocateWorkflow.getArrivalTime(); //PCP�����翪ʼʱ��
			for(ConstraintWTask ConWTask: selectedPcP.get(0).getParentTaskList())
			{
				if(ConWTask.getWTask().getFinishTimeWithConfidency() > startTimeWithConfidency)
				{
					startTimeWithConfidency = ConWTask.getWTask().getFinishTimeWithConfidency();
				}
			}
			
			double baseExecutionTimeForPCP = 0; //PCP����������Ļ�׼ʱ��֮��
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
			{//ΪPCPѰ�����������ʱ�������������ʱ��������				
				double tempStartTimeWF = startTimeWithConfidency; //���ܵĿ�ʼʱ��				
				if(tempVm.getReadyTime() > tempStartTimeWF)
				{
					tempStartTimeWF = tempVm.getReadyTime();
				}
				
				double tempStartTimeWithCon = tempStartTimeWF;
				double finishTimeWithConfidency = StaticfinalTags.currentTime;
				for(WTask tempTask: selectedPcP)
				{//��PCP��ÿһ��������в���
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
				{//ѡ��һ̨������õ������
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
			{//���������
				int vmId = acitveVMs.size() + offVMs.size(); //���������ID
				//???ȷ��һ�������
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
			{//��PCP�е�����������䵽selectedVm��
				double startTimeWF = startTimeWithConfidency; //����ʼʱ��ķ�λ��
				if(selectedVm.getWTaskList().size() != 0)
				{
					if(selectedVm.getReadyTime() > startTimeWF)
					{
						startTimeWF = selectedVm.getReadyTime();
					}					
				}
				
				int i = 0;				
				for(WTask allocatedTask: selectedPcP)
				{//��������䵽�����
					allocatedTask.setAllocatedFlag(true);
					allocatedTask.setAllocateVm(selectedVm);
					if(i==0)
					{//PCP�п�ʼ����
						if(startTimeWF < (allocatedTask.getBaseStartTime()+allocateWorkflow.getArrivalTime()))
						{
							startTimeWF = allocatedTask.getBaseStartTime()+allocateWorkflow.getArrivalTime();
						}												
						allocatedTask.setStartTimeWithConfidency(startTimeWF);
						double finishTimeWithConfidency = startTimeWF + (int)(allocatedTask.getBaseExecutionTime()*selectedVm.getVmFactor());
						allocatedTask.setFinishTimeWithConfidency(finishTimeWithConfidency);
					}
					else
					{//PCP�зǿ�ʼ����???������
						//????startTimeWC��Ҫ�����޸�
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
				
				//�����������״̬
				selectedVm.getWTaskList().addAll(selectedPcP);
				double readyTime = selectedPcP.get(selectedPcP.size()-1).getFinishTimeWithConfidency();
				selectedVm.setReadyTime(readyTime);
			}//��PCP���䵽���������	
			
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
	
	/**���㹤������ÿ������������������ʼʱ��*/
	public void calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	{
		List<WTask> calculatedTaskList = new ArrayList<WTask>(); //�Ѿ������������		
		while(true)
		{
			for(WTask lTask: aWorkflow.getTaskList())//������������б���һ��
			{
//				if(lTask.getLeastFinishTime() > -1)//��������Ѿ��������������
				if(lTask.getLeastFinishTime() != -1)//��������Ѿ��������������
				{
					continue;
				}
				
	//			double executionTime = lTask.getBaseExecutionTime();
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
					double minFinishTime = Integer.MAX_VALUE;
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
	}//end calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	
	/**�ҳ������������е�PCPs*/
	public void searchPCPsForWorkflows(Workflow newWorkflow)
	{
		List<WTask> assignedWTaskList = new ArrayList<WTask>(); //�Ѿ����뵽ָ��PCP�����񼯺�
		int PcPNum = 1;
		while(assignedWTaskList.size() != newWorkflow.getTaskList().size())
		{//ֱ�����е����񶼷�����󣬲��˳�
			WTask startWTask = null; //��һ��Ѱ��PCP�Ŀ�ʼ����
			double maxBaseFinishTime = -1;
			for(WTask task: newWorkflow.getTaskList()) //�ӳ���������Ѱ��
			{
				if(task.getPCPNum() == -1 && task.getSuccessorTaskList().size() == 0) //û�к�������������ǳ�������
				{//��δ����ĳ�������
					if(task.getBaseFinishTime() > maxBaseFinishTime)
					{
						maxBaseFinishTime = task.getBaseFinishTime();
						startWTask = task;
					}						
				}
			}
			if(startWTask == null) //���û�г�������
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
							
			PcPNum++; //PCP�ı��
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
/*	public SaaSVm scaleUpVm(int vmId, int startTime, int level)
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
	
	/**��������Ļ�׼��ʼʱ�������������*/
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
	}
	
}
