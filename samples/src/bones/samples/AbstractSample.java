package bones.samples;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.threed.jpct.FrameBuffer;
import com.threed.jpct.World;

/** <p>Base class of all Swing based samples. This class uses software renderer.</p> 
 * 
 * <p><b>Note:</b> As this class uses Swing, it does all rendering stuff in AWT thread. 
 * The author suggests this for Swing and software renderer based jPCT applications.
 * The rationale is simple: Both jPCT and Swing are not thread safe and it's much easier
 * to do jPCT stuff in AWT thread rather than doing Swing stuff in non-AWT thread.</p>
 * 
 * @author hakan eryargi (r a f t)
 * */
public abstract class AbstractSample {
	protected static final long GRANULARITY = 25; // milliseconds 
	
	protected final FrameBuffer frameBuffer;
	protected final World world = new World();
	protected final RenderPanel renderPanel;
	protected final Dimension size;
	
	private long lastTime = System.currentTimeMillis();
	private long aggregatedTime = 0;

	protected boolean drawWireFrame = false;
	protected boolean drawTextures = true;
	protected Color wireFrameColor = Color.GREEN;

	protected AbstractSample() {
		this(new Dimension(1024, 768));
	}
	
	protected AbstractSample(Dimension size) {
		this.size = size;
		this.frameBuffer = new FrameBuffer(size.width, size.height, FrameBuffer.SAMPLINGMODE_NORMAL);
		this.renderPanel = new RenderPanel(frameBuffer);
		
		renderPanel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_ESCAPE:
						System.exit(0);
						break;
					case KeyEvent.VK_W:
						drawWireFrame = !drawWireFrame;
						break;
					case KeyEvent.VK_T:
						drawTextures = !drawTextures;
						break;
				}
			}
		});
	}
	
	protected String getName() {
		return getClass().getName();
	}
	
	protected abstract void initialize() throws Exception;
	
	protected abstract void update(long deltaTime);

	/** should be called in AWT thread */
	protected void render() {
		frameBuffer.clear();
		world.renderScene(frameBuffer);
		if (drawTextures)
			world.draw(frameBuffer);
        
        if (drawWireFrame)
        	world.drawWireframe(frameBuffer, wireFrameColor);
	}

	protected void display() {
        renderPanel.paintImmediately(renderPanel.getBounds());
	}

	
	public void loop() throws Exception {

		initialize();
		showInFrame();
		
		lastTime = System.currentTimeMillis();
		
    	while (true) {
	        SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
                    long frameStartTime = System.currentTimeMillis();
                    aggregatedTime += (frameStartTime - lastTime);
                    lastTime = frameStartTime;

                    while(aggregatedTime > GRANULARITY) {
                        aggregatedTime -= GRANULARITY;
                        update(GRANULARITY);
                    }

                    render();
                    display();
                    long frameTime = System.currentTimeMillis() - frameStartTime;

                    if (frameTime < GRANULARITY) {
                        try {
                            Thread.sleep(GRANULARITY - frameTime);
                        } catch (InterruptedException ie) {}
                    } else {
                        Thread.yield();
                    }
				}
			});
    	}
	}


    private void showInFrame() {
        JFrame frame = new JFrame(getName());
        frame.setResizable(false);
        frame.add(renderPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    
	
	
}
