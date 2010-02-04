package raft.bones.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.threed.jpct.FrameBuffer;
import com.threed.jpct.World;

public abstract class AbstractDemo {
	protected static final long GRANULARITY = 25; // milliseconds 
	
	protected final FrameBuffer frameBuffer;
	protected final World world = new World();
	protected final RenderPanel renderPanel;
	protected final Dimension size;
	
	private long lastTime = System.currentTimeMillis();
	private long aggregatedTime = 0;

	protected boolean drawWireFrame = false;
	protected Color wireFrameColor = Color.GREEN;

	protected AbstractDemo() {
		this(new Dimension(800, 600));
	}
	
	protected AbstractDemo(Dimension size) {
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
				}
			}
		});
	}
	
	protected abstract String getName();
	
	protected abstract void initialize() throws Exception;
	
	protected abstract void update(long deltaTime);

	/** should be called in AWT thread */
	protected void render() {
		frameBuffer.clear();
        world.renderScene(frameBuffer);
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
