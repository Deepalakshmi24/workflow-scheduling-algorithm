package main;

import share.*;
import workflow.WorkflowProducer;

public class AutoExperiment 
{
	private static final int n = 5; //�����ĸ���
	private static final int AlCount = 3; //�㷨�ĸ���
	/**����*/
	private static double totalCost[][]= new double [AlCount][n];//��ŷ��õĽ��
	private static double costUpper[][]= new double [AlCount][n];//��ŷ����Ͻ�
	private static double costLower[][]= new double [AlCount][n];//��ŷ����½�
	
	/**��Դ������ */
	private static double resourceUtilization[][] = new double [AlCount][n]; //�����Դ�����ʵĽ��	
	private static double RUUpper[][] = new double [AlCount][n]; //�����Դ�������Ͻ�
	private static double RULower[][] = new double [AlCount][n]; //�����Դ�������½�
	
	/**�����̶�*/
	private static double deviation[][] = new double [AlCount][n]; //���ȷ����Ĳ�����	
	private static double deviationUpper[][] = new double [AlCount][n]; //���ȷ����Ĳ������Ͻ�
	private static double deviationLower[][] = new double [AlCount][n]; //���ȷ����Ĳ������½�
	
	public static void main(String[] args) throws Exception
	{				
		int repeatTime = 30; //ÿ��ʵ��Ĵ���
		int variableIndex = -1; //�����ƶ��ı��

		for (int count = 1000; count <= 5001; count = count + 1000)
//		for(double variance=0.1; variance<=0.46; variance=variance+0.05)
//		for(double deadline=1.5; deadline<=5.1; deadline=deadline+0.5)	
//		for(double arrivalRate=0.01; arrivalRate<=0.5; arrivalRate=arrivalRate+0.01)
//		for(int idleTimeSlot=100; idleTimeSlot<=3600; idleTimeSlot=idleTimeSlot+100)
		{
			variableIndex++; //���� �ƶ�������
			StaticfinalTags.workflowNum = count;
//			StaticfinalTags.standardDeviation = variance;
//			StaticfinalTags.deadlineBase = deadline;
//			StaticfinalTags.arrivalLamda = arrivalRate;
//			StaticfinalTags.maxIdleTime = idleTimeSlot;
			
			for (int i=0; i<AlCount; i++)//��ʼ����Ž���Ŀռ�
			{
				//��ʼ������
				totalCost[i][variableIndex] = 0; 
				costUpper[i][variableIndex] = 0;
				costLower[i][variableIndex] = Double.MAX_VALUE;
				
				//��ʼ����Դ������
				resourceUtilization [i][variableIndex] = 0; 
				RUUpper [i][variableIndex] = 0; 
				RULower [i][variableIndex] = 1;
				 
				deviation [i][variableIndex] = 0;
				deviationUpper [i][variableIndex] = 0;
				deviationLower [i][variableIndex] = Double.MAX_VALUE;
			}
			
			for (int r=0; r<repeatTime; r++)//�ظ�ʵ��
			{	
				System.out.println("����������: "+StaticfinalTags.workflowNum+" ʱ�ĵ�  "+(r+1)+" �ε��� Starting");
				System.out.println();
				WorkflowProducer.main(null); //����������				
				for (int i=0; i<AlCount; i++)//ʹ����ͬ�����ݶԲ�ͬ�㷨����ʵ��
				{
					System.out.println("��ʼ�� "+ (i+1) +" ���㷨");
					
					long startSimulationTime = System.currentTimeMillis();
					
					StaticfinalTags.choose = i; //ѡ���㷨
					WorkflowExperiment.main(null);
					
					long endSimulationTime = System.currentTimeMillis();
					System.out.println("Simulation Time: "+(endSimulationTime-startSimulationTime));
					
					
					totalCost[i][variableIndex] += PerformanceValue.TotalCost;					
					resourceUtilization [i][variableIndex] += PerformanceValue.ResourceUtilization;
					deviation [i][variableIndex] += PerformanceValue.deviation;
					
					//����
					if(costUpper [i][variableIndex] < PerformanceValue.TotalCost)
					{//���·����Ͻ�
						costUpper [i][variableIndex] = PerformanceValue.TotalCost;
					}
					if(costLower [i][variableIndex] > PerformanceValue.TotalCost)
					{//���·����½�
						costLower [i][variableIndex] = PerformanceValue.TotalCost;
					}
					
					//��Դ������
					if(RUUpper [i][variableIndex] < PerformanceValue.ResourceUtilization)
					{//������Դ�������Ͻ�
						RUUpper [i][variableIndex] = PerformanceValue.ResourceUtilization;
					}
					if(RULower [i][variableIndex] > PerformanceValue.ResourceUtilization)
					{//������Դ�������½�
						RULower [i][variableIndex] = PerformanceValue.ResourceUtilization;
					}
					
					//�����̶�
					if(deviationUpper [i][variableIndex] < PerformanceValue.deviation)
					{//���²����̶ȵ��Ͻ�
						deviationUpper [i][variableIndex] = PerformanceValue.deviation;
					}
					if(deviationLower [i][variableIndex] > PerformanceValue.deviation)
					{//���²����̶ȵ��½�
						deviationLower [i][variableIndex] = PerformanceValue.deviation;
					}
										
					
					System.out.println("������ "+ (i+1) +" ���㷨");
					System.out.println();
				}				
				System.out.println("����������: "+StaticfinalTags.workflowNum+" , ʱ�ĵ�  "+(r+1)+"  ��ʵ��");
				System.out.println();
			}//һ�������µ�һ��ʵ�����
			
			for (int i=0; i < AlCount; i++) //�Խ��������ƽ��ֵ
			{
				totalCost[i][variableIndex] = totalCost[i][variableIndex]/repeatTime;
				resourceUtilization[i][variableIndex] = resourceUtilization[i][variableIndex]/repeatTime;
				deviation[i][variableIndex] = deviation[i][variableIndex]/repeatTime;
			}
		}//�������Խ���
				
		//ʵ���������	
		java.text.DecimalFormat fd = new java.text.DecimalFormat("0.0000");//�趨������С�����λ��		
		System.out.println("totalCost:");
		for (int i=0; i<AlCount; i++)
		{
			System.out.println("�� "+i+" ���㷨�Ľ��:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format(totalCost[i][j] )+"  ");
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println("costUpper:");
		for (int i=0; i<AlCount; i++)
		{
			System.out.println("�� "+i+" ���㷨�Ľ��:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format(costUpper[i][j] )+"  "); 
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println("costLower:");
		for (int i=0; i<AlCount; i++)
		{
			System.out.println("�� "+i+" ���㷨�Ľ��:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format(costLower[i][j] )+"  "); 
			}
			System.out.println();
		}
		
		
		System.out.println();
		System.out.println();
		System.out.println("Resource Utilization:");
		for (int i=0; i<AlCount; i++)
		{
			System.out.println("�� "+i+" ���㷨�Ľ��:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format( resourceUtilization[i][j] )+"  ");
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println();
		System.out.println("RUUpper:");
		for (int i=0; i<AlCount; i++)
		{
			System.out.println("�� "+i+" ���㷨�Ľ��:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format( RUUpper[i][j] )+"  ");
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println();
		System.out.println("RULower:");
		for (int i=0; i<AlCount; i++)
		{
			System.out.println("�� "+i+" ���㷨�Ľ��:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format( RULower[i][j] )+"  ");
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println();
		System.out.println("Deviation:");
		for (int i=0; i < AlCount; i++)
		{
			System.out.println("�� "+i+" ���㷨�Ľ��:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format( deviation[i][j] )+"  ");
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println("deviationUpper:");
		for (int i=0; i < AlCount; i++)
		{
			System.out.println("�� "+i+" ���㷨�Ľ��:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format( deviationUpper[i][j] )+"  ");
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println("deviationLower:");
		for (int i=0; i < AlCount; i++)
		{
			System.out.println("�� "+i+" ���㷨�Ľ��:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format( deviationLower[i][j] )+"  ");
			}
			System.out.println();
		}

	}//end main()
}//end AutoExperiment
