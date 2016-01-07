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

/**�㷨ʵ�֣���̬�������������������䵽�������*/
public class ScheduleAlgorithms 
{
	private List<SaaSVm> vmList; //���������
	private List<Workflow> workflowList; //����������
	
	public ScheduleAlgorithms() throws Exception
	{
		this.vmList = new ArrayList<SaaSVm>(); //ֻ������������Ϊ��
		initialVmList(StaticfinalTags.initialVmNum); //��ʼ��ϵͳ�е������
		this.workflowList = new ArrayList<Workflow>(); //��ʼ���������б�
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	/**
	 * ���ȶ�̬��������SaaS�������
	 */
	public void scheduleDynamicWorkflowToSaaSVmByPRS()
	{
		System.out.println("Algorithm PRS is Started");
		List<Workflow> workflowList = getWorkflowList();
		//����ÿ��������һ�����Ŷ��µ�ִ��ʱ��
		calculateTaskBaseExecutionTimeWithConfidency(workflowList);
				
		int vmId = getVmList().size(); //ȫ���������ID
		List<SaaSVm> activeVmList = new ArrayList<SaaSVm>(); //ϵͳ�л�Ծ�����
		activeVmList = getVmList();		
		List<SaaSVm> offVmList = new ArrayList<SaaSVm>(); //ϵͳ���Ѿ��رյ������		
		List<WTask> RH_WTask = new ArrayList<WTask>(); //����أ�����ȫ�ֵȴ�����
		
		for(int i=0; i<workflowList.size(); i++) //�����й��������е���
		{
//			System.out.println("Time: "+workflowList.get(i).getArrivalTime()+" workflow: "+i+" Name: "+workflowList.get(i).getWorkflowName());
			//��ͬʱ����Ĺ������ҳ���
			List<Workflow>  workflowsArriveSynchronously = new ArrayList<Workflow>(); //���ͬʱ����Ĺ�����
			workflowsArriveSynchronously.add(workflowList.get(i));
			StaticfinalTags.currentTime = workflowList.get(i).getArrivalTime(); //����ϵͳ�ĵ�ǰʱ��
			for(int k=i+1; k<workflowList.size(); k++) //Ѱ��ͬʱ����Ĺ�����
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
			}//�Ѿ��ҳ�ͬʱ����Ĺ�����
						
			for(Workflow rankWorkflow: workflowsArriveSynchronously)
			{//Ϊÿ���������Ȩ��				
				calculateWorkflowTaskLeastTime(rankWorkflow); //����ÿ��������������ʱ�������ʼʱ��
				rankWTasksForPRS(rankWorkflow); //��������������ʱ����Ϊ�������Ȩ��
			}//����Ȩ�ؽ���
			
			//��ready������з���
			List<WTask> readyWTaskList = new ArrayList<WTask>();
			readyWTaskList = getReadyWTaskFromNewWorkflows(workflowsArriveSynchronously);
			
			//������������ȵ��������
			scheduleReadyWTaskToSaaSVM(readyWTaskList, activeVmList, vmId);
			vmId = activeVmList.size() + offVmList.size();
												
			for(Workflow addWorkflow: workflowsArriveSynchronously)
			{//��δ���ȵ�������뵽RH_WTask��
				for(WTask addTask: addWorkflow.getTaskList())
				{
					if(!addTask.getAllocatedFlag()) //����addTask��δ������
					{
						RH_WTask.add(addTask);
					}
				}
			}
			
			int nextArrivalTime = Integer.MAX_VALUE; //��һ�������������ʱ��
			if(i != workflowList.size()-1)
			{
				nextArrivalTime = workflowList.get(i+1).getArrivalTime();
			}
			
			//����������б���  ��turnOffVmTime �� nextFinishTime��ֵ
			double nextFinishTime = Integer.MAX_VALUE; //ÿ����Ҫ����ǰ��Ҫ���������ĸ�ֵ
			SaaSVm nextFinishVm = null; //��Ҫ��������ִ������״̬�������
			double turnOffVmTime = Integer.MAX_VALUE; //ÿ����Ҫ����ǰ��Ҫ���������ĸ�ֵ
			SaaSVm turnOffVm = null; //��Ҫɾ���������
			for(SaaSVm initiatedVm: activeVmList)
			{//��ÿ����������б���
				double tempFinishTime = initiatedVm.getExecutingWTask().getRealFinishTime();
				if(tempFinishTime != -1)//�������������ִ�е�����
				{
					if (tempFinishTime < nextFinishTime)//�ҳ�  �����������ִ��������������ʱ��
					{
						nextFinishTime = tempFinishTime;
						nextFinishVm = initiatedVm;
					}
				}
				else //������������е����
				{
					//����������ܹرյ�ʱ��
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
					if (tempTurnOffTime < turnOffVmTime)//�ҳ� ������ر�ʱ�̵������
					{
						turnOffVmTime = tempTurnOffTime;
						turnOffVm = initiatedVm;//��Ҫ�رյ������
					}
					
					if(initiatedVm.getWaitWTaskList().size() != 0)
					{//???ɾ��
						throw new IllegalArgumentException("Error: there exists waiting task on idle VM!");
					}
				}
			}//�������������
			
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%	
			//���������ڵ��﹤����֮��������¼����и���, ��������1�������������ִ���������ɣ���2�� VM�����ҵ������ӣ��رո��������
			while(nextArrivalTime >= nextFinishTime || 
					nextArrivalTime > turnOffVmTime)
			{								
				if(nextFinishTime <= turnOffVmTime)
				{//����  ��һ���������
					WTask finishTask = nextFinishVm.getExecutingWTask();					
					//���µ�ǰ��ʱ��, ���������������ִ��������������ʱ��ȷ��
					StaticfinalTags.currentTime = nextFinishVm.getExecutingWTask().getRealFinishTime();
					if(nextFinishVm.getWaitWTaskList().size() != 0)
					{//��������еȴ�����
						nextFinishVm.getExecutingWTask().setFinishFlag(true);
						WTask nextExecutionTask = nextFinishVm.getWaitWTaskList().get(0);			
						nextFinishVm.setExecutingWTask(nextExecutionTask);
						nextFinishVm.getWaitWTaskList().remove(nextExecutionTask); //��ִ�е�����ӵȴ��������Ƴ�
					}
					else
					{//������ϲ����ڵȴ�����
						nextFinishVm.getExecutingWTask().setFinishFlag(true);
						nextFinishVm.setExecutingWTask(new WTask());
					}
					
/*					if(nextFinishVm.getWaitingWTask().getBaseExecutionTime() != -1)
					{//������������ڵȴ�����
						nextFinishVm.getExecutingWTask().setFinishFlag(true);
						nextFinishVm.setExecutingWTask(nextFinishVm.getWaitingWTask());
						//???�ĳɸ��µȴ�����
						nextFinishVm.setWaitingWTask(new WTask());					
					}
					else
					{//�������û�����ڵȴ�����
						nextFinishVm.getExecutingWTask().setFinishFlag(true);
						nextFinishVm.setExecutingWTask(new WTask());
					}*/
					
					//�ҳ���������
					List<WTask> readySucessorList = getReadySucessorsInRH(finishTask);					
					//������������ȵ��������
					scheduleReadyWTaskToSaaSVM(readySucessorList, activeVmList, vmId);
					vmId = activeVmList.size() + offVmList.size();					
					//��������������RH���Ƴ�
					RH_WTask.removeAll(readySucessorList);									
				}//���µ� �������ʱ��  ����
																						
				if(turnOffVmTime < nextFinishTime)
				{//�ر������, turnOffVmTimeΪ��ǰ��ʱ��
					StaticfinalTags.currentTime = turnOffVmTime; //����ϵͳ��ǰ��ʱ��
					double workTime = turnOffVmTime - turnOffVm.getVmStartWorkTime();					
					double cost = (workTime*turnOffVm.getVmPrice())/3600;
					
					turnOffVm.setEndWorkTime(turnOffVmTime);
					turnOffVm.setTotalCost(cost);
					turnOffVm.setVmStatus(false);
					activeVmList.remove(turnOffVm);
					offVmList.add(turnOffVm);																									
				}//�ر����������																								
								
				//����������б���  ��turnOffVmTime �� nextFinishTime��ֵ
				nextFinishTime = Integer.MAX_VALUE; //ÿ����Ҫ����ǰ��Ҫ���������ĸ�ֵ
				nextFinishVm = null; //��Ҫ��������ִ������״̬�������
				turnOffVmTime = Integer.MAX_VALUE; //ÿ����Ҫ����ǰ��Ҫ���������ĸ�ֵ
				turnOffVm = null; //��Ҫɾ���������
				for(SaaSVm initiatedVm: activeVmList)
				{//��ÿ����������б���
					double tempFinishTime = initiatedVm.getExecutingWTask().getRealFinishTime();
					if(tempFinishTime != -1)//�������������ִ�е�����
					{
						if (tempFinishTime < nextFinishTime)//�ҳ�  �����������ִ��������������ʱ��
						{
							nextFinishTime = tempFinishTime;
							nextFinishVm = initiatedVm;
						}
					}
					else //��������е����
					{
						//����������ܹرյ�ʱ��
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
						if (tempTurnOffTime < turnOffVmTime)//�ҳ� ������ر�ʱ�̵������
						{
							turnOffVmTime = tempTurnOffTime;
							turnOffVm = initiatedVm;//��Ҫ�رյ������
						}
						
						if(initiatedVm.getWaitWTaskList().size() != 0)
						{//???ɾ��
							throw new IllegalArgumentException("Error: there exists waiting task on idle VM!");
						}
					}
				}//�������������	
				
				if(nextArrivalTime==Integer.MAX_VALUE && nextFinishTime==Integer.MAX_VALUE 
						&& turnOffVmTime==Integer.MAX_VALUE)
				{
					break;
				}
			}//end while, ���������ڵ��﹤����֮��������¼����и���  һ���ֻؽ���			
		  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		}//end for(int i=0; i<workflowList.size(); i++) //�����й��������е���
		
		//������Ƚ��
//		verifyTheScheduleResult(workflowList);
		//�����������Ƿ����ͬʱ���е�����
//		verifyWTaskOnSaaSVm(offVmList);
						
		if(activeVmList.size() != 0)
		{//ʵ�����Ӧ��û�л�Ծ�����
			System.out.println("Error: there exists active VMs at last!");
		}
		
		java.text.DecimalFormat fd = new java.text.DecimalFormat("0.0000");//�趨������С�����λ��
		//ͳ�Ƶ���ָ�꣺(1)���ã�(2)��Դ�����ʣ�(3)������
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
		
		//ʵ���������	
		PerformanceValue.TotalCost = totalCost;
		PerformanceValue.ResourceUtilization = reUtilization;
		PerformanceValue.deviation = workflowDeviation;
		System.out.println("Total Cost: "+fd.format(totalCost)+" Resource Utilization: "+fd.format(reUtilization)+" Deviation: "+fd.format(workflowDeviation));		
		
		workflowList.clear();
		offVmList.clear();
		activeVmList.clear();		
	}//end public void scheduleDynamicWorkflowToSaaSVmByPRS()
	
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
	
	
	/**��֤����ĵ��Ƚ���Ƿ�����(1)��ֹ��Ҫ��;(2)�����ǰ��Լ��*/
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
	
	
	/**�������������ʼʱ�������������*/
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
	
	/**��������Ļ�׼ִ��ʱ�������������*/
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
	
	/**��ȡȫ�ֶ����еľ������ȵ�����*/
/*	public List<WTask> getReadyScheduledWTaskInRH(List<WTask> WTaskList)
	{
		List<WTask> readyWTaskList = new ArrayList<WTask>();
		for(WTask task: WTaskList)
		{
			if(task.getParentTaskList().size() == 0)
			{//û�и����������Ϊ��������
				readyWTaskList.add(task);
			}
			else
			{//�и�����������
				boolean ready = true; //����urgentTask�Ƿ�Ϊ�����ı�־
				for(ConstraintWTask parentConWTask: task.getParentTaskList())
				{
					if(!parentConWTask.getWTask().getAllocatedFlag())
					{//�������δ�����ȵĸ�������ô�������Ǿ�������
						ready = false;
					}
				}
				if(ready) //�������urgentTaskΪ������������
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
	
	/**��ȡȫ�ֵȴ������еľ�������*/
/*	public List<WTask> getReadyWTaskInRH(List<WTask> WTaskList)
	{
		List<WTask> readyWTaskList = new ArrayList<WTask>();
		for(WTask task: WTaskList)
		{
			if(task.getParentTaskList().size() == 0)
			{//û�и����������Ϊ��������
				readyWTaskList.add(task);
			}
			else
			{//�и���������
				boolean ready = true; //����urgentTask�Ƿ�Ϊ�����ı�־
				for(ConstraintWTask parentConWTask: task.getParentTaskList())
				{
					if(!parentConWTask.getWTask().getFinishFlag())
					{//�������δ�����ȵĸ�������ô�������Ǿ�������
						ready = false;
					}
				}
				if(ready) //�������urgentTaskΪ������������
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
	
	/**�ҳ�����finishTask�ľ�����������new*/
	public List<WTask> getReadySucessorsInRH(WTask finishTask)
	{
		List<WTask> readyWTaskList = new ArrayList<WTask>();
		for(ConstraintWTask succ: finishTask.getSuccessorTaskList())
		{						
			WTask succWTask = succ.getWTask(); //��������
			if(succWTask.getAllocatedFlag())
			{//����������Ѿ����ȹ���������
				continue;
			}
			boolean ready = true;
			for(ConstraintWTask parent: succWTask.getParentTaskList())
			{//������е�ǰ�������Ѿ������ȣ���ô������ͳ�Ϊ����
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
	
	
	/**������������ȵ��������new*/
	public void scheduleReadyWTaskToSaaSVM(List<WTask> taskList, List<SaaSVm> vmList, int vmID)
	{
		//�Ծ���������Ȩ�ؽ������� WTaskComparatorByTaskBaseExecutionTimeIncrease
		Collections.sort(taskList, new WTaskComparatorByLeastStartTimeIncrease()); //�Ծ��������������
//		Collections.sort(taskList, new WTaskComparatorByTaskBaseExecutionTimeIncrease()); //�Ծ��������������
		for(WTask scheduleTask: taskList) 
		{//��ÿ��������е���
			double minCost = Double.MAX_VALUE;
			double startTimeWithConfi = Double.MAX_VALUE;
			SaaSVm targetVm = null;			
			for(SaaSVm vm: vmList)
			{//Ѱ�������: �����Ϊ����,����������ĵȴ�����Ϊ��				
				if(!vm.getWaitingWTask().getTaskId().equals("initial"))
				{
					throw new IllegalArgumentException("Error: there exists waiting task!");
				}
				
				//��ǰ�����������ʱ��ķ�λ��������������
				Collections.sort(scheduleTask.getParentTaskList(), new PredWTaskComparatorByFinishTimeConfiIncrease());				
				double startTime = StaticfinalTags.currentTime; //�������vm�ϵĿ�ʼʱ��
				for(ConstraintWTask parentCon: scheduleTask.getParentTaskList())
				{//�������ÿ��ǰ��������м��
					SaaSVm parentTaskVm = parentCon.getWTask().getAllocateVm(); //ǰ���������ڵ������
					double minFinishTransTime = Double.MAX_VALUE;
					CommPort destinationPort = null;						
					
					//ͨ��ƿ���Ĵ���
					double minBandwidth = vm.getCommPortList().get(0).getBandwidth();
					if(minBandwidth > StaticfinalTags.bandwidth)
					{
						minBandwidth = StaticfinalTags.bandwidth;
					}
					
					if(!parentTaskVm.equals(vm)) //ǰ�������뵱ǰ������ͬһ������ϵ����
					{//�������ݴ���: ��ǰ��������ͬһ������ϵ����, Դ�����Ѿ��ر�												
						for(CommPort port: vm.getCommPortList())
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
						CommRecord planRecord = new CommRecord(null, vm, parentCon.getDataSize(), 
								minBandwidth, startTransTime, minFinishTransTime);
						destinationPort.getPlanCommRecordList().add(planRecord);
					}//����һ��ǰ���������
					else //ǰ�������뵱ǰ������ͬһ������ϵ����
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
					{//��������Ŀ�ʼʱ��
						startTime = minFinishTransTime;														
					}
					
					if(!parentCon.getWTask().getAllocatedFlag())
					{//��ǰ������δ�����
						throw new IllegalArgumentException("There exists unfinished parent task!");
					}
				}//ȷ����������翪ʼʱ��				
				for(CommPort port: vm.getCommPortList())
				{//����ͨ�Žӿ���Ԥ���õ�ͨ��
					port.getPlanCommRecordList().clear();
				}
				
				double tempAvailableTime = StaticfinalTags.currentTime;
				if(!vm.getExecutingWTask().getTaskId().equals("initial"))
				{//�������޸�  ������ϻ�������ִ����������
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
				{//����һ���Ŀ���ʱ�䣬������
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
			}//����for(SaaSVm vm: vmList) Ѱ�����������
			
			if(targetVm != null)//����������������ҵ����ʵ������
			{				
				//��������scheduleTask��״̬					
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
									
				//���������vm��״̬
				targetVm.setFinishTime(finishTimeWithConfidency);
				targetVm.setRealFinishTime(realFinishtTime);
				targetVm.setReadyTime(finishTimeWithConfidency);
				targetVm.getWTaskList().add(scheduleTask);
				
				if(targetVm.getExecutingWTask().getTaskId().equals("initial"))
				{//���õ��������ִ��
					targetVm.setExecutingWTask(scheduleTask);
				}
				else
				{//���õ�������ϵȴ�
					targetVm.getWaitWTaskList().add(scheduleTask);
//					targetVm.setWaitingWTask(scheduleTask);
				}
			}
			else //û�ҵ����ʵ������
			{//�����µ������				
				SaaSVm newVm = addSaaSVmAndAllocateWTask(vmID, scheduleTask); //���������
				if(newVm == null){ throw new IllegalArgumentException("Error: can not add a new VM!");}
				vmID++;
				vmList.add(newVm);							
			}
		}//end for(WTask scheduleTask: taskList) //��ÿ��������е���
	}//���Ⱦ����������
			
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
		{//����һ�����������
			newVm = scaleUpVm(vmID, startTime00, 0);
			startTime = startTime00;
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
		newVm.setFinishTime(finishTimeWithConfidency);
		newVm.setRealFinishTime(realFinishtTime);
		newVm.setReadyTime(finishTimeWithConfidency);
		newVm.getWTaskList().add(task);
		
		if(newVm.getExecutingWTask().getTaskId().equals("initial"))
		{//���õ��������ִ��
			newVm.setExecutingWTask(task);
		}
		else
		{//���õ�������ϵȴ�
			newVm.setWaitingWTask(task);
			throw new IllegalArgumentException("Error: There exists execution task on new SaaSVm!");
		}
										
		return newVm;
	}
	
	/**ȷ���������������������ϵ����翪ʼʱ��new*/
	public double determineStartTimeForReadyTask(WTask task, int vmType)
	{
		double startTime = StaticfinalTags.currentTime;
		
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
		
		//��ǰ�����������ʱ��ķ�λ��������������
		Collections.sort(task.getParentTaskList(), new PredWTaskComparatorByFinishTimeConfiIncrease());
		
		for(ConstraintWTask parentCon: task.getParentTaskList())
		{//�������ÿ��ǰ��������м��
			double minFinishTransTime = Double.MAX_VALUE;
			CommPort destinationPort = null;						
									
			for(CommPort port: commPortList)
			{//ȷ��ͨ�Žӿ�
				double startCommTime = StaticfinalTags.currentTime; //��ʼͨ��ʱ��
				if(parentCon.getWTask().getRealFinishTime() > startCommTime)
				{
					startCommTime = parentCon.getWTask().getFinishTimeWithConfidency();
				}
								
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
		double realStartTime = StaticfinalTags.currentTime;		
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
					double startCommTime = StaticfinalTags.currentTime;
					if(parentCon.getWTask().getRealFinishTime() > startCommTime)
					{
						startCommTime = parentCon.getWTask().getRealFinishTime();
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
										
				double startTransTime = minFinishTransTime - parentCon.getDataSize()/minBandwidth;
				CommRecord planRecord = new CommRecord(null, targetVm, parentCon.getDataSize(), 
						minBandwidth, startTransTime, minFinishTransTime);
				destinationPort.getPlanCommRecordList().add(planRecord);
			}//����һ��ǰ���������
			else //ǰ�������뵱ǰ������ͬһ������ϵ����
			{//����Ҫ���ݴ���ʱ��
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
			{//��������Ŀ�ʼʱ��
				realStartTime = minFinishTransTime;														
			}
		}//ȷ����������翪ʼʱ��				
		for(CommPort port: targetVm.getCommPortList())
		{//����ͨ�Žӿ���Ԥ���õ�ͨ��
			port.getPlanCommRecordList().clear();
		}
		
		if(!targetVm.getExecutingWTask().getTaskId().equals("initial"))
		{//???�޸ģ������ȴ���������������
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
				{//???ȥ��
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
	
	/**��ȡ�µ��﹤�����ľ������񣬼�û��ǰ�����������new*/
	public List<WTask> getReadyWTaskFromNewWorkflows(List<Workflow> newWorkFlows)
	{
		List<WTask> readyTaskList = new ArrayList<WTask>();
		for(Workflow newWorkflow: newWorkFlows)
		{//��ÿ���µ��﹤�������б���				
			for(WTask tempWTask: newWorkflow.getTaskList())
			{
				if(tempWTask.getParentTaskList().size() == 0)
				{//�����µ��﹤������û��ǰ�������������Ǿ�������
					readyTaskList.add(tempWTask);
				}
			}
		}
		return readyTaskList;
	}
	
	/**��ȡ�����ĵȴ�����w*/
/*	public List<WTask> getReadyWTaskList(List<Workflow> waitWorkFlows)
	{
		List<WTask> readyTaskList = new ArrayList<WTask>(); 
		
		for(Workflow roundWorkflow: waitWorkFlows) 
		{//��ÿ�����������б���
			for(WTask tempWTask: roundWorkflow.getTaskList())
			{//�Թ������е�ÿ��������б���
				if(!tempWTask.getAllocatedFlag())
				{//�ж�����tempWTask�Ƿ��Ѿ�����
					boolean ready = true;
					List<ConstraintWTask> parentTaskList = new ArrayList<ConstraintWTask>();
					parentTaskList.addAll(tempWTask.getParentTaskList());
					for(ConstraintWTask parentConstraint: parentTaskList)
					{//???????���������⣬Ӧ���Ǹ�������ɵ�������Ǿ�������
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
	}//��ȡ����������� */
	
	/**���ù������������Ȩ�أ�����ʼʱ����Ϊ�����Ȩ�أ�Ӧ�����ȿ��ǵ���Ȩ��С������*/
	public void rankWTasksForPRS(Workflow aWorkflow)
	{
		for(WTask rankTask: aWorkflow.getTaskList())
		{
			double rank = rankTask.getLeastFinishTime();
			rankTask.setPriority(rank);
		}
	}
	
	/**���㹤������ÿ������������������ʼʱ��*/
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
			
			if(calculatedTaskList.size() == aWorkflow.getTaskList().size())//�����������
			{
				break;
			}
		}//end while
	}//end calculateWorkflowTaskLeastTime(Workflow aWorkflow)
	
	/**����������һ�����Ŷ��µĻ�׼ִ��ʱ��*/
	public static void calculateTaskBaseExecutionTimeWithConfidency(List<Workflow> list)
	{//������Ӧ����չΪ��ͬ�ֲ�
		for(Workflow tempWorkflow: list)
		{//��ÿ�����������б���
			for(WTask tempTask: tempWorkflow.getTaskList())
			{//��ÿ���������е�ÿ��������б���
				double standardDeviation = tempTask.getBaseExecutionTime()*StaticfinalTags.standardDeviation;				
				double executionTimeWithConfidency = tempTask.getBaseExecutionTime() + standardDeviation*getQuantile(StaticfinalTags.confidency);
				tempTask.setExecutionTimeWithConfidency(executionTimeWithConfidency);
			}
		}
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
	
	/**��ȡ��λ����only level = 0.7 0.75 0.8 0.85 0.9 0.95 0.99 can be gained��*/
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
