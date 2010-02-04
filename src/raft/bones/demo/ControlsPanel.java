package raft.bones.demo;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import raft.jpct.bones.SkinnedGroup;
import raft.jpct.bones.Skeleton;
import raft.jpct.bones.Skinned3D;

public class ControlsPanel extends JPanel implements KeyListener {
	private static final long serialVersionUID = 1L;

	private final SkinnedGroup skinnedGroup;
    private final Skeleton.Debugger skeletonDebugger;
	
	private boolean showMesh = true;
	private boolean showSkeleton = true;
	
	public ControlsPanel(SkinnedGroup skinnedGroup, Skeleton.Debugger skeletonDebugger) {
		this.skinnedGroup = skinnedGroup;
		this.skeletonDebugger = skeletonDebugger;
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		
		add(createLabel("Arrow keys, A,Z to move camera", Color.WHITE));
		add(createLabel("S: toggle skeleton", Color.WHITE));
		add(createLabel("M: toggle mesh group", Color.WHITE));
		if (skinnedGroup.getSize() > 1) {
			add(createLabel("0-" + (skinnedGroup.getSize() - 1) + ": toggle mesh", Color.WHITE));
		}
		
		for (Skinned3D o : skinnedGroup) {
			o.setVisibility(showMesh);
		}
		skeletonDebugger.setVisibility(showSkeleton);
	}
	
	private JLabel createLabel(String text, Color color) {
		JLabel label = new JLabel(text);
		label.setForeground(color);
		return label;
	}

	private void toggleVisible(int index) {
		if ((skinnedGroup.getSize() > 1) && (index < skinnedGroup.getSize()))
			skinnedGroup.get(index).setVisibility(!skinnedGroup.get(index).getVisibility());
	}
	
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_S:
				showSkeleton = !showSkeleton;
				skeletonDebugger.setVisibility(showSkeleton);
				break;
			case KeyEvent.VK_M:
				showMesh = !showMesh;
				for (Skinned3D o : skinnedGroup) {
					o.setVisibility(showMesh);
				}
				break;
			case KeyEvent.VK_0:
			case KeyEvent.VK_1:
			case KeyEvent.VK_2:
			case KeyEvent.VK_3:
			case KeyEvent.VK_4:
			case KeyEvent.VK_5:
			case KeyEvent.VK_6:
			case KeyEvent.VK_7:
			case KeyEvent.VK_8:
			case KeyEvent.VK_9:
				toggleVisible(e.getKeyCode() - KeyEvent.VK_0);
				break;
		}
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}

	
}
