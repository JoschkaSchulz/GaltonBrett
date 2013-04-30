import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.p5.Physics;

import processing.core.PApplet;

public class TestPinata extends PApplet {
	
	private Physics physics;
	private Body body;
	
	private int x = 0;
	private int y = 0;
	
	public void setup() {
		size(600, 480);
		
		physics = new Physics(this, width, height);
		physics.setDensity(1.0f);
		body = physics.createRect(300,200,340,300);
	}
	
	public void keyPressed() {
		switch(keyCode) {
			default:
				System.out.println("Ich habe keine Ahnung welche Taste gedrückt wurde :D"+keyCode);
				break;
			case 39: //Rechts
				x++;
				break;
			case 37: //Links
				x--;
				break;
			case 32:
				physics.applyForce(body, 100.0f, 0f);
				break;
		}
	}
	
	public void draw() {		
		
		
		background(255, 255, 255);
	}
	
}
