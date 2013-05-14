import org.jbox2d.collision.ShapeType;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.ContactEdge;
import org.jbox2d.p5.Physics;

import processing.core.PApplet;


public class CollisionTest extends PApplet {

	private Physics physics;
	private World world;
	
	private Body b1, b2, b3;
	
	public void setup() {
		size(640,480);
		frameRate(60);
		smooth();
		
		physics = new Physics(this, 640, 480);
		world = physics.getWorld();
		
		physics.setDensity(5.0f);
		physics.setFriction(0.0f);
		physics.setRestitution(0.0f);
		
		physics.createCircle(50, 240, 15);
		
		physics.setDensity(0.2f);
		physics.setFriction(0.5f);
		physics.setRestitution(0.2f);
		b1 = physics.createRect(550, 400, 600, 450);
		b2 = physics.createRect(500, 400, 550, 450);
		b3 = physics.createRect(525, 350, 575, 400);
		
		physics.setDensity(15.0f);
		physics.setFriction(0.2f);
		physics.setRestitution(0.0f);
		physics.createPolygon(5,400,
								5,470,
								180,470);
	}
	
	public void checkCollision() {
		Body body;
		for (body = world.getBodyList(); body != null; body = body.getNext()) {
			
			org.jbox2d.collision.Shape shape;
			for (shape = body.getShapeList(); shape != null; shape = shape.getNext()) {
				if(shape.getType() == ShapeType.CIRCLE_SHAPE) {
					ContactEdge ce;
					for(ce = body.getContactList(); ce != null; ce = ce.next) {
						
					}
				}
			}
		}
	}
	
	public void draw() {
		background(255);
		
		checkCollision();
	}
}
