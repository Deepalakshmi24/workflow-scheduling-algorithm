package main;

import share.*;
import workflow.WorkflowProducer;

public class AutoExperiment 
{
	private static final int n = 5; //变量的个数
	private static final int AlCount = 3; //算法的个数
	/**费用*/
	private static double totalCost[][]= new double [AlCount][n];//存放费用的结果
	private static double costUpper[][]= new double [AlCount][n];//存放费用上界
	private static double costLower[][]= new double [AlCount][n];//存放费用下界
	
	/**资源利用率 */
	private static double resourceUtilization[][] = new double [AlCount][n]; //存放资源利用率的结果	
	private static double RUUpper[][] = new double [AlCount][n]; //存放资源利用率上界
	private static double RULower[][] = new double [AlCount][n]; //存放资源利用率下界
	
	/**波动程度*/
	private static double deviation[][] = new double [AlCount][n]; //调度方案的波动性	
	private static double deviationUpper[][] = new double [AlCount][n]; //调度方案的波动性上界
	private static double deviationLower[][] = new double [AlCount][n]; //调度方案的波动性下界
	
	public static void main(String[] args) throws Exception
	{				
		int repeatTime = 30; //每组实验的次数
		int variableIndex = -1; //变量移动的标记

		for (int count = 1000; count <= 5001; count = count + 1000)
//		for(double variance=0.1; variance<=0.46; variance=variance+0.05)
//		for(double deadline=1.5; deadline<=5.1; deadline=deadline+0.5)	
//		for(double arrivalRate=0.01; arrivalRate<=0.5; arrivalRate=arrivalRate+0.01)
//		for(int idleTimeSlot=100; idleTimeSlot<=3600; idleTimeSlot=idleTimeSlot+100)
		{
			variableIndex++; //变量 移动的索引
			StaticfinalTags.workflowNum = count;
//			StaticfinalTags.standardDeviation = variance;
//			StaticfinalTags.deadlineBase = deadline;
//			StaticfinalTags.arrivalLamda = arrivalRate;
//			StaticfinalTags.maxIdleTime = idleTimeSlot;
			
			for (int i=0; i<AlCount; i++)//初始化存放结果的空间
			{
				//初始化费用
				totalCost[i][variableIndex] = 0; 
				costUpper[i][variableIndex] = 0;
				costLower[i][variableIndex] = Double.MAX_VALUE;
				
				//初始化资源利用率
				resourceUtilization [i][variableIndex] = 0; 
				RUUpper [i][variableIndex] = 0; 
				RULower [i][variableIndex] = 1;
				 
				deviation [i][variableIndex] = 0;
				deviationUpper [i][variableIndex] = 0;
				deviationLower [i][variableIndex] = Double.MAX_VALUE;
			}
			
			for (int r=0; r<repeatTime; r++)//重复实验
			{	
				System.out.println("工作流数量: "+StaticfinalTags.workflowNum+" 时的第  "+(r+1)+" 次迭代 Starting");
				System.out.println();
				WorkflowProducer.main(null); //产生工作流				
				for (int i=0; i<AlCount; i++)//使用相同的数据对不同算法进行实验
				{
					System.out.println("开始第 "+ (i+1) +" 个算法");
					
					long startSimulationTime = System.currentTimeMillis();
					
					StaticfinalTags.choose = i; //选择算法
					WorkflowExperiment.main(null);
					
					long endSimulationTime = System.currentTimeMillis();
					System.out.println("Simulation Time: "+(endSimulationTime-startSimulationTime));
					
					
					totalCost[i][variableIndex] += PerformanceValue.TotalCost;					
					resourceUtilization [i][variableIndex] += PerformanceValue.ResourceUtilization;
					deviation [i][variableIndex] += PerformanceValue.deviation;
					
					//费用
					if(costUpper [i][variableIndex] < PerformanceValue.TotalCost)
					{//更新费用上界
						costUpper [i][variableIndex] = PerformanceValue.TotalCost;
					}
					if(costLower [i][variableIndex] > PerformanceValue.TotalCost)
					{//更新费用下界
						costLower [i][variableIndex] = PerformanceValue.TotalCost;
					}
					
					//资源利用率
					if(RUUpper [i][variableIndex] < PerformanceValue.ResourceUtilization)
					{//更新资源利用率上界
						RUUpper [i][variableIndex] = PerformanceValue.ResourceUtilization;
					}
					if(RULower [i][variableIndex] > PerformanceValue.ResourceUtilization)
					{//更新资源利用率下界
						RULower [i][variableIndex] = PerformanceValue.ResourceUtilization;
					}
					
					//波动程度
					if(deviationUpper [i][variableIndex] < PerformanceValue.deviation)
					{//更新波动程度的上界
						deviationUpper [i][variableIndex] = PerformanceValue.deviation;
					}
					if(deviationLower [i][variableIndex] > PerformanceValue.deviation)
					{//更新波动程度的下界
						deviationLower [i][variableIndex] = PerformanceValue.deviation;
					}
										
					
					System.out.println("结束第 "+ (i+1) +" 个算法");
					System.out.println();
				}				
				System.out.println("工作流数量: "+StaticfinalTags.workflowNum+" , 时的第  "+(r+1)+"  次实验");
				System.out.println();
			}//一个参数下的一组实验完成
			
			for (int i=0; i < AlCount; i++) //对结果进行求平均值
			{
				totalCost[i][variableIndex] = totalCost[i][variableIndex]/repeatTime;
				resourceUtilization[i][variableIndex] = resourceUtilization[i][variableIndex]/repeatTime;
				deviation[i][variableIndex] = deviation[i][variableIndex]/repeatTime;
			}
		}//变量测试结束
				
		//实验结果的输出	
		java.text.DecimalFormat fd = new java.text.DecimalFormat("0.0000");//设定输出结果小数点的位数		
		System.out.println("totalCost:");
		for (int i=0; i<AlCount; i++)
		{
			System.out.println("第 "+i+" 个算法的结果:");
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
			System.out.println("第 "+i+" 个算法的结果:");
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
			System.out.println("第 "+i+" 个算法的结果:");
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
			System.out.println("第 "+i+" 个算法的结果:");
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
			System.out.println("第 "+i+" 个算法的结果:");
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
			System.out.println("第 "+i+" 个算法的结果:");
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
			System.out.println("第 "+i+" 个算法的结果:");
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
			System.out.println("第 "+i+" 个算法的结果:");
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
			System.out.println("第 "+i+" 个算法的结果:");
			for (int j=0; j<n; j++)
			{
				System.out.print(fd.format( deviationLower[i][j] )+"  ");
			}
			System.out.println();
		}

	}//end main()
}//end AutoExperiment
