package bones.samples;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import com.threed.jpct.FrameBuffer;

/** 
 * <p>Simple Swing panel to display {@link FrameBuffer} contents.</p>
 * 
 * @author hakan eryargi (r a f t)
 * */
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
    }
    
}

