import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.jbox2d.collision.CircleShape;
import org.jbox2d.collision.PolygonShape;
import org.jbox2d.collision.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.jbox2d.p5.Physics;
import org.json.*;

import processing.core.PApplet;
import processing.core.PImage;

public class TestPinata extends PApplet {

	class UserData {
		public int image;
		public String name = "Random Guest";
		
		public UserData() {
		};
	}

	private final static int BLAUES_MONSTER1 = 1;
	private final static int PINKES_MONSTER1 = 2;
	private final static int ORANGES_MONSTER1 = 3;

	private final static int LEERTASTE = 32;
	private final static int LINKS = 37;
	private final static int HOCH = 38;
	private final static int RECHTS = 39;
	private final static int RUNTER = 40;
	private final static int A_TASTE = 65;
	private final static int D_TASTE = 68;

	private Physics physics;
	private World world;
	
	private int crosshairX = 300;
	private int crosshairY = 35;

	private int points = 0;
	
	private PImage img, img2, img3;
	private int time;
	
	public void setup() {
		size(800, 480);
		frameRate(60);
		smooth();

		time = millis();
		
		img = loadImage("blaues_monster1.png");
		img.resize(32, 32);

		img2 = loadImage("pinkes_monster1.png");
		img2.resize(32, 32);
		
		img3 = loadImage("oranges_monster1.png");
		img3.resize(32, 32);
		
		initScene();

		// physics.setDensity(1.0f);
		// body = physics.createRect(175, 100, 200, 50);
		
		createWorld();
		
		twitterReader();
	}

	public void twitterReader() {
		String urlst = "http://search.twitter.com/search.json?q=%23PiniataCookie";
		URL url;
		StringBuffer buff = new StringBuffer(); 
		try {
			url = new URL(urlst);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
			
			int c;  
	        while((c=br.read())!=-1)  
	        {  
	            buff.append((char)c);  
	        }  
	        br.close();
	        
	        System.out.println(buff.toString());
	        
	        JSONObject js = new JSONObject(buff.toString());
	        JSONArray tweets = js.getJSONArray("results");  
	        JSONObject tweet;  
	        for(int i=0;i<tweets.length();i++) {  
	            tweet = tweets.getJSONObject(i);  
	            System.out.println((i+1)+") "+tweet.getString("from_user")+" at "+tweet.getString("created_at"));  
	            System.out.println(tweets.getJSONObject(i).getString("text")+"\n");  
		    } 
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
	
	public void createWorld() {
		 physics.setDensity(0.0f);
		 for(int i = 0; i < 13; i++) {
			 if(i*50 < 550) {
				 physics.setRestitution(random(0.5f,1.5f));
				 physics.createCircle(50+(i*50), 125, 5);
				 
				 physics.setRestitution(random(0.5f,1.5f));
				 physics.createCircle(25+(i*50), 200, 5);
				 
				 physics.setRestitution(random(0.5f,1.5f));
				 physics.createCircle(50+(i*50), 275, 5);
				 
				 physics.setRestitution(random(0.5f,1.5f));
				 physics.createCircle(25+(i*50), 350, 5);
			 }
		 }
	}
	public void initScene() {
		physics = new Physics(this, width-200, height);
//		physics.createHollowBox(250, 250, 50, 50, 5);
		world = physics.getWorld();
		// physics = new Physics(this, width, height, 0.0f, -9.81f, width,
		// height, width, height-100, 1.0f);
		physics.setCustomRenderingMethod(this, "myCustomRenderer");
	}

	public void followText(String text, int x, int y, int width, int height) {
		// Ermitteln welche Rand am weitesten entfernt ist
		int abstand = 50;

		int xAchse = constrain(x - abstand, abstand, width - 50);
		int yAchse = constrain(y - abstand, abstand, height - 50);

		int textsize = (int)textWidth(text)+10;
		fill(255,255,255);
		line(xAchse - 5, yAchse+3, x, y);
		line(xAchse - 5 + textsize + 5, yAchse+3, x, y);
		rect(xAchse - 5, yAchse - 12, textsize + 5, 15);
		fill(0);
		text(text, xAchse, yAchse);
		noFill();
	}
	
	public void myCustomRenderer(World world) {

		// clear the background
		background(255);//0x666666);

		if( ((time+10000)-millis()) <= 500) {
			twitterReader();
			time = millis();
		}
		
		crosshair();
		drawPoints();
		
		// Show the gravity
		stroke(255, 128, 0);
		Vec2 g = world.getGravity();
		line(width / 2.0f, height / 2.0f, (width / 2.0f) + g.x, (height / 2.0f)
				- g.y);
		fill(255, 128, 0);
		ellipse((width / 2.0f), (height / 2.0f), 4, 4);
		
		//Anzeige rechts ab 600px
		fill(0);
		stroke(0);
		text("Gravity X:"+g.x+"m/s² Y:"+-(g.y)+"m/s²", 605, 10);
		text("Punkte: "+points, 605, 20);
		
		noFill();
		
		// iterate through the bodies
		Body body;
		for (body = world.getBodyList(); body != null; body = body.getNext()) {

			// iterate through the shapes of the body
			org.jbox2d.collision.Shape shape;
			for (shape = body.getShapeList(); shape != null; shape = shape
					.getNext()) {

				// find out the shape type
				ShapeType st = shape.getType();
				if (st == ShapeType.POLYGON_SHAPE) {

					// polygon? let's iterate through its vertices while using
					// begin/endShap()
					beginShape();
					PolygonShape poly = (PolygonShape) shape;
					int count = poly.getVertexCount();
					Vec2[] verts = poly.getVertices();
					for (int i = 0; i < count; i++) {
						Vec2 vert = physics.worldToScreen(body
								.getWorldPoint(verts[i]));
						vertex(vert.x, vert.y);
					}
					Vec2 firstVert = physics.worldToScreen(body
							.getWorldPoint(verts[0]));
					vertex(firstVert.x, firstVert.y);
					endShape();
					// Vec2 v = ((PolygonShape) shape).getBody().getPosition();
					// image(img, physics.worldToScreen(v.x),
					// physics.worldToScreen(-v.y));

				} else if (st == ShapeType.CIRCLE_SHAPE) {
					UserData ud = (UserData) body.getUserData();
					// circle? let's find its center and radius and draw an
					// ellipse
					CircleShape circle = (CircleShape) shape;
					Vec2 pos = physics.worldToScreen(body.getWorldPoint(circle
							.getLocalPosition()));
					float radius = physics.worldToScreen(circle.getRadius());

					checkPoints(world,pos, body);
					
					if (ud != null) {					
						switch (ud.image) {
						default:
							ellipseMode(CENTER);
							ellipse(pos.x, pos.y, radius * 2, radius * 2);
							// we'll add one more line to see how it rotates
							line(pos.x, pos.y,
									pos.x + radius * cos(-body.getAngle()),
									pos.y + radius * sin(-body.getAngle()));
							break;
						case BLAUES_MONSTER1:
							pushMatrix();
							translate(pos.x, pos.y);
							rotate(-(radius * body.getAngle()) * TWO_PI / 360);
							image(img, -(img.width / 2), -(img.height / 2));
							rotate(radius * body.getAngle() * TWO_PI / 360);
							translate(-(pos.x), -(pos.y));
							popMatrix();
							break;
						case PINKES_MONSTER1:
							pushMatrix();
							translate(pos.x, pos.y);
							rotate(-(radius * body.getAngle()) * TWO_PI / 360);
							image(img2, -(img2.width / 2), -(img2.height / 2));
							rotate(radius * body.getAngle() * TWO_PI / 360);
							translate(-(pos.x), -(pos.y));
							popMatrix();
							break;
						case ORANGES_MONSTER1:
							pushMatrix();
							translate(pos.x, pos.y);
							rotate(-(radius * body.getAngle()) * TWO_PI / 360);
							image(img3, -(img3.width / 2), -(img3.height / 2));
							rotate(radius * body.getAngle() * TWO_PI / 360);
							translate(-(pos.x), -(pos.y));
							popMatrix(); 
							break;
						}
					} else {
						ellipseMode(CENTER);
						ellipse(pos.x, pos.y, radius * 2, radius * 2);
						// we'll add one more line to see how it rotates
						line(pos.x, pos.y,
								pos.x + radius * cos(-body.getAngle()), pos.y
										+ radius * sin(-body.getAngle()));
					}
				}
			}
		}
		// iterate through the bodies
		for (body = world.getBodyList(); body != null; body = body.getNext()) {

			// iterate through the shapes of the body
			org.jbox2d.collision.Shape shape;
			for (shape = body.getShapeList(); shape != null; shape = shape
					.getNext()) {

				// find out the shape type
				ShapeType st = shape.getType();
				if (st == ShapeType.CIRCLE_SHAPE) {
					UserData ud = (UserData) body.getUserData();
					// circle? let's find its center and radius and draw an
					// ellipse
					CircleShape circle = (CircleShape) shape;
					Vec2 pos = physics.worldToScreen(body.getWorldPoint(circle
							.getLocalPosition()));
					float radius = physics.worldToScreen(circle.getRadius());
					
					if(ud != null) {
						followText(ud.name, (int)pos.x, (int)pos.y, 640, 480);
					}
				}
			}
		}
	}

	private void crosshair() {
		noFill();
		rect(crosshairX-5,crosshairY-5,10,10);
		line(crosshairX,crosshairY,crosshairX,crosshairY+30);
		line(crosshairX,crosshairY,crosshairX,crosshairY-30);
		line(crosshairX,crosshairY,crosshairX+30,crosshairY);
		line(crosshairX,crosshairY,crosshairX-30,crosshairY);
	}
	
	private void createRandom(int x, int y) {
		UserData ud = new UserData();
		ud.image = (int) (Math.random() * 3)+1;

		physics.setDensity(1.0f);
		physics.setFriction(1.0f);
		physics.setRestitution(0.7f);
		physics.createCircle(x, y, 15).setUserData(ud);
	}

	private void drawPoints() {
		//100 Punkte :D
		fill(255, 0, 0);
		rect(250, 400, 100, 80);
		stroke(0);
		fill(0);
		text("100 Punkte",260,425);
		
		//50 Punkte :D
		fill(255, 64, 0);
		rect(150, 400, 100, 80);
		stroke(0);
		fill(0);
		text("50 Punkte",160,425);

		fill(255, 64, 0);
		rect(350, 400, 100, 80);
		stroke(0);
		fill(0);
		text("50 Punkte",360,425);
		
		//25 Punkte :D
		fill(255, 128, 0);
		rect(50, 400, 100, 80);
		stroke(0);
		fill(0);
		text("25 Punkte",60,425);
		
		fill(255, 128, 0);
		rect(450, 400, 100, 80);
		stroke(0);
		fill(0);
		text("25 Punkte",460,425);
		
		//0 Punkte :D
		fill(255, 255, 0);
		rect(0, 400, 50, 80);
		stroke(0);
		fill(0);
		text("Keine",10,425);
		
		fill(255, 255, 0);
		rect(550, 400, 50, 80);
		stroke(0);
		fill(0);
		text("Keine",560,425);
	}
	
	public void checkPoints(World world, Vec2 pos, Body body) {
		
		//100 Punkte :D
//		fill(255, 0, 0, 30);
//		rect(250, 400, 100, 80);
		Vec2 p100p = new Vec2(250,400);	// p[oints]100p[osition]
		Vec2 p100s = new Vec2(100,80);	// p[oints]100s[ize]
//		stroke(0);
//		fill(0);
//		text("100 Punkte",260,425);
		
		//50 Punkte :D
//		fill(255, 64, 0, 30);
//		rect(150, 400, 100, 80);
		Vec2 p50p1 = new Vec2(150,400);	// p[oints]50p[osition]1
		Vec2 p50s1 = new Vec2(100,80);	// p[oints]50s[ize]1
//		stroke(0);
//		fill(0);
//		text("50 Punkte",160,425);
//		
//		fill(255, 64, 0, 30);
//		rect(350, 400, 100, 80);
		Vec2 p50p2 = new Vec2(350,400);	// p[oints]50p[osition]1
		Vec2 p50s2 = new Vec2(100,80);	// p[oints]50s[ize]1
//		stroke(0);
//		fill(0);
//		text("50 Punkte",360,425);
		
//		//25 Punkte :D
//		fill(255, 128, 0, 30);
//		rect(50, 400, 100, 80);
		Vec2 p25p1 = new Vec2(50,400);	// p[oints]25p[osition]1
		Vec2 p25s1 = new Vec2(100,80);	// p[oints]25s[ize]1
//		stroke(0);
//		fill(0);
//		text("25 Punkte",60,425);
//		
//		fill(255, 128, 0, 30);
//		rect(450, 400, 100, 80);
		Vec2 p25p2 = new Vec2(450,400);	// p[oints]25p[osition]1
		Vec2 p25s2 = new Vec2(100,80);	// p[oints]25s[ize]1
//		stroke(0);
//		fill(0);
//		text("25 Punkte",460,425);
		
//		//0 Punkte :D
//		fill(255, 255, 0, 30);
//		rect(0, 400, 50, 80);
		Vec2 p0p1 = new Vec2(0,400);	// p[oints]25p[osition]1
		Vec2 p0s1 = new Vec2(50,80);	// p[oints]25s[ize]1
//		stroke(0);
//		fill(0);
//		text("Keine",10,425);
//		
//		fill(255, 255, 0, 30);
//		rect(550, 400, 50, 80);
		Vec2 p0p2 = new Vec2(550,400);	// p[oints]25p[osition]1
		Vec2 p0s2 = new Vec2(50,80);	// p[oints]25s[ize]1
//		stroke(0);
//		fill(0);
//		text("Keine",560,425);		
		
		if(collision(pos, p100p, p100s)) {
			world.destroyBody(body);
			points += 100;
		}else if(collision(pos, p50p1, p50s1) || collision(pos, p50p2, p50s2)) {
			world.destroyBody(body);
			points += 50;
		}else if(collision(pos, p25p1, p25s1) || collision(pos, p25p2, p25s2)) {
			world.destroyBody(body);
			points += 25;
		}else if(collision(pos, p0p1, p0s1) || collision(pos, p0p2, p0s2)) {
			world.destroyBody(body);
			points += 0;
		}
	}
	
	public boolean collision(Vec2 objPos, Vec2 tarPos, Vec2 tarSize) {
		return (objPos.x > tarPos.x && objPos.x < tarPos.x + tarSize.x &&
					objPos.y > tarPos.y && objPos.y < tarPos.y + tarSize.y); 
	}
	
	public void keyPressed() {

		Vec2 gravity = world.getGravity();

		switch (keyCode) {
		default:
			System.out
					.println("Ich habe keine Ahnung welche Taste gedrückt wurde :D"
							+ keyCode);
			break;
		case RECHTS:
			world.setGravity(new Vec2(gravity.x + 10, gravity.y));
			break;
		case LINKS:
			world.setGravity(new Vec2(gravity.x - 10, gravity.y));
			break;
		case HOCH:
			world.setGravity(new Vec2(gravity.x, gravity.y + 10));
			break;
		case RUNTER:
			world.setGravity(new Vec2(gravity.x, gravity.y - 10));
			break;
		case A_TASTE:
			crosshairX -= 5;
			break;
		case D_TASTE:
			crosshairX += 5;
			break;
		case LEERTASTE:
			createRandom(crosshairX, crosshairY);
			break;
		case 18:
			physics.setDensity(1.0f);
			physics.setFriction(1.0f);
			physics.setRestitution(0.7f);
			physics.createCircle(205, 150, 15);
			break;
		}
	}

	public void draw() {
		background(255f, 15f);

		// Show the gravity
		stroke(255, 0, 0);
		Vec2 g = world.getGravity();
		line(width / 2.0f, height / 2.0f, (width / 2.0f) + g.x, (height / 2.0f)
				- g.y);
		fill(255, 0, 0);
		ellipse((width / 2.0f), (height / 2.0f), 4, 4);
		
		text("Gravity X:"+g.x+" Y:"+g.y, 605, 10);

	}

}
