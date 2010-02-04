package raft.bones.demo;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import com.threed.jpct.FrameBuffer;

public class RenderPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private final FrameBuffer frameBuffer;
	
	public RenderPanel(FrameBuffer frameBuffer) {
		this.frameBuffer = frameBuffer;

		setPreferredSize(new Dimension(frameBuffer.getOutputWidth(), frameBuffer.getOutputHeight()));
		setFocusable(true);
		setLayout(null);
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				requestFocus();
			}
		});
	}
    
	@Override
    public void paintComponent(Graphics g) {
        frameBuffer.display(g);
        
//        g.setColor(Color.WHITE);
//        
//        int x = 10; int y = 20;
//        g.drawString("arrow keys, a,z to move camera", x, y+=20);
//        g.drawString("vis list: " + world.getVisibilityList().getSize(), x, y+=20);
//        g.drawString("w: toggle wireframe " + (drawWireFrame ? "(visible)" : "(hidden)"), x, y+=20);
//        g.drawString("s: toggle skeleton " + (showSkeleton ? "(visible)" : "(hidden)"), x, y+=20);
//        g.drawString("m: toggle mesh " + (showMesh ? "(visible)" : "(hidden)"), x, y+=20);
        
    }
    
}

