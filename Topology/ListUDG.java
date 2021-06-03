/*package stream;*/
 
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ListUDG {
    // �ڽӱ��б��Ӧ������Ķ���
    private class ENode {
        int ivex;       // �ñ���ָ��Ķ����λ��
        ENode nextEdge; // ָ����һ������ָ��
    }

    // �ڽӱ��б�Ķ���
    private class VNode {
        int port;          // ����˿�
        ENode firstEdge;    // ָ���һ�������ö���Ļ�
    };

    private VNode[] mVexs;  // ��������

	 /*
     * ��node�ڵ����ӵ�list�����
     */
    private void linkLast(ENode list, ENode node) {
        ENode p = list;

        while(p.nextEdge!=null)
            p = p.nextEdge;
        p.nextEdge = node;
    }
	

	public ListUDG(int[] vexs, int[][] edges) {

		// ��ʼ��"������"��"����"
		int vlen = vexs.length;
		int elen = edges.length;

		// ��ʼ��"����"
		mVexs = new VNode[vlen];
		for (int i = 0; i < mVexs.length; i++) {
			mVexs[i] = new VNode();
			mVexs[i].port = vexs[i];
			mVexs[i].firstEdge = null;
		}

		// ��ʼ��"��"
		for (int i = 0; i < elen; i++) {
			// ��ȡ�ߵ���ʼ����ͽ�������
			int p1 = edges[i][0];
			int p2 = edges[i][1];
			// ��ʼ��node1
			ENode node1 = new ENode();
			node1.ivex = p2;
			// ��node1���ӵ�"p1���������ĩβ"
			if(mVexs[p1].firstEdge == null)
			  mVexs[p1].firstEdge = node1;
			else
				linkLast(mVexs[p1].firstEdge, node1);
			// ��ʼ��node2
			ENode node2 = new ENode();
			node2.ivex = p1;
			// ��node2���ӵ�"p2���������ĩβ"
			if(mVexs[p2].firstEdge == null)
			  mVexs[p2].firstEdge = node2;
			else
				linkLast(mVexs[p2].firstEdge, node2);

		}
	}


	/**
     * ����ͼ���ڽӱ�
     * @param graph �������ͼ
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
            // ׼���ļ�lol2.txt���е������ǿյ�
            FileWriter f = new FileWriter("AdjacencyList.txt");
 
            // ���������ļ��������
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
				// ������д�뵽�����
				//fos.write(data);
				// �ر������
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