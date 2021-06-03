/*package stream;*/
 
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ListUDG {
    // 邻接表中表对应的链表的顶点
    private class ENode {
        int ivex;       // 该边所指向的顶点的位置
        ENode nextEdge; // 指向下一条弧的指针
    }

    // 邻接表中表的顶点
    private class VNode {
        int port;          // 顶点端口
        ENode firstEdge;    // 指向第一条依附该顶点的弧
    };

    private VNode[] mVexs;  // 顶点数组

	 /*
     * 将node节点链接到list的最后
     */
    private void linkLast(ENode list, ENode node) {
        ENode p = list;

        while(p.nextEdge!=null)
            p = p.nextEdge;
        p.nextEdge = node;
    }
	

	public ListUDG(int[] vexs, int[][] edges) {

		// 初始化"顶点数"和"边数"
		int vlen = vexs.length;
		int elen = edges.length;

		// 初始化"顶点"
		mVexs = new VNode[vlen];
		for (int i = 0; i < mVexs.length; i++) {
			mVexs[i] = new VNode();
			mVexs[i].port = vexs[i];
			mVexs[i].firstEdge = null;
		}

		// 初始化"边"
		for (int i = 0; i < elen; i++) {
			// 读取边的起始顶点和结束顶点
			int p1 = edges[i][0];
			int p2 = edges[i][1];
			// 初始化node1
			ENode node1 = new ENode();
			node1.ivex = p2;
			// 将node1链接到"p1所在链表的末尾"
			if(mVexs[p1].firstEdge == null)
			  mVexs[p1].firstEdge = node1;
			else
				linkLast(mVexs[p1].firstEdge, node1);
			// 初始化node2
			ENode node2 = new ENode();
			node2.ivex = p1;
			// 将node2链接到"p2所在链表的末尾"
			if(mVexs[p2].firstEdge == null)
			  mVexs[p2].firstEdge = node2;
			else
				linkLast(mVexs[p2].firstEdge, node2);

		}
	}


	/**
     * 输入图的邻接表
     * @param graph 待输出的图
     */
    public void outputGraph(){
        /*for(int i=0;i<mVexs.length;i++){
            System.out.print(mVexs[i].port);
			if (mVexs[i].firstEdge != null)
			{
				System.out.print("-->"+mVexs[mVexs[i].firstEdge.ivex].port);
				ENode current=mVexs[i].firstEdge.nextEdge;
				while(current!=null){
					System.out.print("-->"+mVexs[current.ivex].port);
					current=current.nextEdge;
				}
			}
            
            System.out.println();
			*/
		try {
            // 准备文件lol2.txt其中的内容是空的
            FileWriter f = new FileWriter("AdjacencyList.txt");
 
            // 创建基于文件的输出流
            //FileOutputStream fos = new FileOutputStream(f);
			for(int i=0;i<mVexs.length;i++){
				f.write(Integer.toString(mVexs[i].port));
				if (mVexs[i].firstEdge != null)
				{
					f.write("-->"+Integer.toString(mVexs[mVexs[i].firstEdge.ivex].port));
					ENode current=mVexs[i].firstEdge.nextEdge;
					while(current!=null){
						f.write("-->"+Integer.toString(mVexs[current.ivex].port));
						current=current.nextEdge;
					}
				}
				f.write("\n");
			}
				// 把数据写入到输出流
				//fos.write(data);
				// 关闭输出流
			f.close();
			
             
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}

	public static void main(String[] args) {
		int[] vexs = new int[]{1546, 1217, 1697, 1810, 1075, 1600, 1840, 1615, 1212, 1298};
		int[][] edges = new int[][]{
			{0,3}, 
			{1,3}, 
			{2,3}, 
			{3,6}, 
			{4,6}, 
			{5,6}, 
			{6,9},
			{7,9},
			{8,9}};
		ListUDG pG = new ListUDG(vexs, edges);
		pG.outputGraph();
		
		
		
    }
}