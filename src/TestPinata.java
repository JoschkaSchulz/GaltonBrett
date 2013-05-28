/*
 * @name: TestPinata
 * @author: Carola Christoffel & Joschka Schulz
 * @description:
 * 		Unser kleines Spiel für das Reperbahnfestival.
 * 
 * @changelog:
 * 		(noch nicht im Blog) 23.05.2013:
 * 			-Twitter Reader wird nun innerhalb eines Threads ausgeführt
 * 			-Bug mit Gravity nach Zero Gravtiy gefixed
 */

import java.awt.Font;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.jbox2d.collision.CircleShape;
import org.jbox2d.collision.PolygonShape;
import org.jbox2d.collision.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.jbox2d.p5.Physics;
import org.json.*;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

public class TestPinata extends PApplet {

	class UserData {
		public int image;
		public String text = "Lorem Ipsum";
		public String name = "Random Guest";
		
		public UserData() {
		};
	}
	
	class Highscore {
		LinkedList<Integer> points = new LinkedList<>();
		LinkedList<String> name = new LinkedList<>();
		
		public void addPlayer(String name, int points) {
			if(this.points.size() <= 0) {
				this.points.add(points);
				this.name.add(name);
			}else {
				if(this.name.contains(name)) {
					int index = this.name.indexOf(name);
					this.points.remove(index);
					this.name.remove(index);
				}
				
				boolean check = false;
				for(int i = 0; i < constrain(this.points.size(), 1, 10); i++) {
					if(points > this.points.get(i)) {
						this.points.add(i, points);
						this.name.add(i,name);
						check = true;
						
						if(this.points.size() > 10) {
							this.points.removeLast();
							this.name.removeLast();
						}
						break;
					}
				}
				
				if(!check && this.points.size() < 10) {
					this.points.addLast(points);
					this.name.addLast(name);
				}
			}
		}
		
		public LinkedList<Integer> getPoints() {
			return points;
		}
		
		public LinkedList<String> getName() {
			return name;
		}
		
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
	private int time, throwtime;

	private HashMap<String, Integer> playerScores;
	
	private Highscore highscore;
	
	private PFont font;
	
	public static final int SCREEN_W = 1920;
	public static final int SCREEN_H = 1080;
	
	public void setup() {
		//size(320,160);
		//size(800,480);
		//size(800,800);	//Quadratisch Praktisch Gut :D
		size(TestPinata.SCREEN_W, TestPinata.SCREEN_H);
		frameRate(60);
		smooth(8);

		tweets = new LinkedList<>();
		playerScores = new HashMap<>();
		highscore = new Highscore();
		
		time = throwtime = millis();
		
		img = loadImage("blaues_monster1.png");
		img.resize(width/32,width/32);//32, 32);

		img2 = loadImage("pinkes_monster1.png");
		img2.resize(width/32,width/32);
		
		img3 = loadImage("oranges_monster1.png");
		img3.resize(width/32,width/32);
		
		//Font
		font = createFont("Audiowide-Regular.ttf",width/64);
		textFont(font);
		
		initScene();

		// physics.setDensity(1.0f);
		// body = physics.createRect(175, 100, 200, 50);
		
		twitterReader();
		
		createWorld();
		MonsterTweetReader mtr = new MonsterTweetReader();
		mtr.start();
	}
	
	public static void main(String[] args) {
		Frame frame = new Frame("Regenbogencookies");
		frame.setUndecorated(true);
		// The name "sketch_name" must match the name of your program
		PApplet applet = new TestPinata(   );
		frame.add(applet);
		applet.init(   );
		frame.setBounds(0, 0, TestPinata.SCREEN_W, TestPinata.SCREEN_H); 
		frame.setVisible(true);
	}
	
	/*
	 * Gibt einen Bereich des Displays zurück zwischen 0% - 100%
	 */
	public float getPerX(float percent) {
		return width/100f*percent;
	}
	
	/*
	 * Gibt einen Bereich des Displays zurück zwischen 0% - 100%
	 */
	public float getPerY(float percent) {
		return height/100f*percent;
	}

	class MonsterTweetReader extends Thread{
		public void run() {
			try {
				while(true) {
					TestPinata.this.twitterReader();
					Thread.sleep(5000);
				}
			}catch(Exception e) {}
		}
	}
	
	class Tweet {
		public String name;
		public String text;
		public void Tweet(String n, String t) {name = n; text = t;}
		public String toString() {
			return name;
		}
	}
	private long lastTweet = 0;
	private LinkedList<Tweet> tweets;
	private boolean firstRun = true;
	
	public void twitterReader() { //PiniataCookie
		//Bieber stress test!
		String urlst = "http://search.twitter.com/search.json?q=Bieber";
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
	        
	        //System.out.println(buff.toString());
	        
	        JSONObject js = new JSONObject(buff.toString());
	        JSONArray tweets = js.getJSONArray("results");  
	        JSONObject tweet;  
	        for(int i=tweets.length()-1;i>=0;i--) {  
	            tweet = tweets.getJSONObject(i);
	            if(lastTweet < Long.parseLong(tweet.getString("id_str"))) {
	            	lastTweet = Long.parseLong(tweet.getString("id_str"));
	            	if(!firstRun) {
	            		Tweet t = new Tweet();
		            	t.name = tweet.getString("from_user");
		            	t.text = tweet.getString("text");
		            	this.tweets.add(t);
	            	}
	            }
		    } 
	        firstRun = false;
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
	
	private float minG = 0.5f;
	private float maxG = 0.95f;
	private float abstand = 6.5f;
	public void createWorld() {
		 physics.setDensity(0.0f);
		 physics.createRect(getPerX(70), getPerY(0), getPerX(100), getPerY(100));
		 System.out.println("x:"+getPerX(60));
		 for(int i = 0; i < 10; i++) {
			 if(i*50 < 550) {
				 physics.setRestitution(random(minG,maxG));
				 physics.createCircle(getPerX(abstand)+getPerX(i*abstand),getPerY(30), width/120);//50+(i*50), 125, 5);
				 
				 if(i > 0){
					 physics.setRestitution(random(minG,maxG));
					 physics.createCircle(getPerX(abstand/2)+getPerX(i*abstand),getPerY(45), width/120);//25+(i*50), 200, 5);
				 }
				 
				 physics.setRestitution(random(minG,maxG));
				 physics.createCircle(getPerX(abstand)+getPerX(i*abstand),getPerY(60), width/120);//50+(i*50), 275, 5);
				 
				 if(i > 0){		
					 physics.setRestitution(random(minG,maxG));
					 physics.createCircle(getPerX(abstand/2)+getPerX(i*abstand),getPerY(75), width/120);//25+(i*50), 350, 5);
				 }
			 }
		 }
	}
	public void initScene() {
		physics = new Physics(this, width, height);
//		physics = new Physics(this, width, height, 0.0f, -9.81f, width, height, width, height-100, 1.0f);
//		physics.createHollowBox(250, 250, 50, 50, 5);
		world = physics.getWorld();
	
		// physics = new Physics(this, width, height, 0.0f, -9.81f, width,
		// height, width, height-100, 1.0f);
		physics.setCustomRenderingMethod(this, "myCustomRenderer");
	}

	public void followText(String text, int x, int y, int width, int height) {
		// Ermitteln welche Rand am weitesten entfernt ist
		int abstand = 50;

		int xAchse = constrain(x - abstand, abstand, width - (int)getPerX(5));
		int yAchse = constrain(y - abstand, abstand, height - (int)getPerY(5));

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
		background(255);//0x666666)

		if( ((time+10000)-millis()) <= 500) {
			System.out.println("--- Highscore ---");
			LinkedList<String> names = this.highscore.getName();
			LinkedList<Integer> points = this.highscore.getPoints();
			
			for(int i = 0; i < names.size(); i++) {
				System.out.println((i+1) + ". Platz: " + names.get(i) + " mit " + points.get(i) + " Punkten");
			}
			time = millis();
		}
		
		if( ((throwtime+6000)-millis()) <= 500) {
			if(this.tweets.size() > 0) {
//				println(Arrays.toString(this.tweets.toArray()));
				Tweet t = this.tweets.getFirst();
				this.tweets.removeFirst();
				createRandomFromTweet(t.name, t.text, (int)random(25f, 400f), 50);
			}
			
			throwtime = millis();
		}
		
//		crosshair();
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
		//text("Gravity X:"+g.x+"m/s² Y:"+-(g.y)+"m/s²", 605, 10);
		
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

					if (ud != null) {		
						checkPoints(world,pos, body, ud);
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
						//Anti Stuck?
						body.applyForce(new Vec2(random(-1, 1),0), new Vec2(0,0));
						
						//Follow Texte
						followText(ud.name, (int)pos.x, (int)pos.y, (int)getPerX(70), (int)getPerY(100));
						
						if(playerScores.get(ud.name) != null) {
							//Twitter Nachrichten
							text(ud.name + "("+playerScores.get(ud.name)+"): " + ud.text, 605, (int)pos.y);
						} else {
							//Twitter Nachrichten
							text(ud.name + "(0): " + ud.text, 605, (int)pos.y);
						}
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
	
	private void createRandomFromTweet(String name, String text, int x, int y) {
		UserData ud = new UserData();
		ud.name = name;
		ud.text = text;
		ud.image = (int) (Math.random() * 3)+1;

		physics.setDensity(1.0f);
		physics.setFriction(1.0f);
		physics.setRestitution(0.7f);
		physics.createCircle(x, y, width/64).setUserData(ud);
	}
	
	private void createRandom(int x, int y) {
		UserData ud = new UserData();
		ud.image = (int) (Math.random() * 3)+1;

		physics.setDensity(1.0f);
		physics.setFriction(1.0f);
		physics.setRestitution(0.7f);
		physics.createCircle(x, y, width/64).setUserData(ud);
	}

	private void drawPoints() {
		//100 Punkte :D
		fill(255, 0, 0);
		rect(getPerX(30),getPerY(90),getPerX(10),getPerY(10));//250, 400, 100, 80);
		stroke(0);
		fill(0);
		text("100",getPerX(33.5f),getPerY(95));
		
		//50 Punkte :D
		fill(255, 64, 0);
		rect(getPerX(20),getPerY(90),getPerX(10),getPerY(10));//150, 400, 100, 80);
		stroke(0);
		fill(0);
		text("50",getPerX(24),getPerY(95));

		fill(255, 64, 0);
		rect(getPerX(40),getPerY(90),getPerX(10),getPerY(10));//350, 400, 100, 80);
		stroke(0);
		fill(0);
		text("50",getPerX(44),getPerY(95));
		
		//25 Punkte :D
		fill(255, 128, 0);
		rect(getPerX(10),getPerY(90),getPerX(10),getPerY(10));//50, 400, 100, 80);
		stroke(0);
		fill(0);
		text("25",getPerX(14),getPerY(95));
		
		fill(255, 128, 0);
		rect(getPerX(50),getPerY(90),getPerX(10),getPerY(10));//450, 400, 100, 80);
		stroke(0);
		fill(0);
		text("25",getPerX(54),getPerY(95));
		
		//0 Punkte :D
		fill(255, 255, 0);
		rect(getPerX(0),getPerY(90),getPerX(10),getPerY(10));//0, 400, 50, 80);
		stroke(0);
		fill(0);
		text("0",getPerX(4),getPerY(95));
		
		fill(255, 255, 0);
		rect(getPerX(60),getPerY(90),getPerX(10),getPerY(10));//550, 400, 50, 80);
		stroke(0);
		fill(0);
		text("0",getPerX(64),getPerY(95));
	}
	
	public void checkPoints(World world, Vec2 pos, Body body, UserData ud) {
		
		//100 Punkte :D
		Vec2 p100p = new Vec2(getPerX(30),getPerY(90));	// p[oints]100p[osition]
		Vec2 p100s = new Vec2(getPerX(10),getPerY(10));	// p[oints]100s[ize]

		Vec2 p50p1 = new Vec2(getPerX(20),getPerY(90));	// p[oints]50p[osition]1
		Vec2 p50s1 = new Vec2(getPerX(10),getPerY(10));	// p[oints]50s[ize]1

		Vec2 p50p2 = new Vec2(getPerX(40),getPerY(90));	// p[oints]50p[osition]1
		Vec2 p50s2 = new Vec2(getPerX(10),getPerY(10));	// p[oints]50s[ize]1

		Vec2 p25p1 = new Vec2(getPerX(10),getPerY(90));	// p[oints]25p[osition]1
		Vec2 p25s1 = new Vec2(getPerX(10),getPerY(10));	// p[oints]25s[ize]1

		Vec2 p25p2 = new Vec2(getPerX(50),getPerY(90));	// p[oints]25p[osition]1
		Vec2 p25s2 = new Vec2(getPerX(10),getPerY(10));	// p[oints]25s[ize]1

		Vec2 p0p1 = new Vec2(getPerX(0),getPerY(90));	// p[oints]0p[osition]1
		Vec2 p0s1 = new Vec2(getPerX(10),getPerY(10));	// p[oints]0s[ize]1

		Vec2 p0p2 = new Vec2(getPerX(60),getPerY(90));	// p[oints]0p[osition]1
		Vec2 p0s2 = new Vec2(getPerX(10),getPerY(10));	// p[oints]0s[ize]1
		
		boolean hasScored = false;
		if(collision(pos, p100p, p100s)) {
			world.destroyBody(body);
			points += 100;
			hasScored = true;
			if(playerScores.get(ud.name) != null) {
				playerScores.put(ud.name, playerScores.get(ud.name) + 100);
			} else {
				playerScores.put(ud.name, 100);
			}
		}else if(collision(pos, p50p1, p50s1) || collision(pos, p50p2, p50s2)) {
			world.destroyBody(body);
			points += 50;
			hasScored = true;
			if(playerScores.get(ud.name) != null) {
				playerScores.put(ud.name, playerScores.get(ud.name) + 50);
			} else {
				playerScores.put(ud.name, 50);
			}
		}else if(collision(pos, p25p1, p25s1) || collision(pos, p25p2, p25s2)) {
			world.destroyBody(body);
			points += 25;
			hasScored = true;
			if(playerScores.get(ud.name) != null) {
				playerScores.put(ud.name, playerScores.get(ud.name) + 25);
			} else {
				playerScores.put(ud.name, 25);
			}
		}else if(collision(pos, p0p1, p0s1) || collision(pos, p0p2, p0s2)) {
			world.destroyBody(body);
			points += 0;
			hasScored = true;
			if(playerScores.get(ud.name) != null) {
				playerScores.put(ud.name, playerScores.get(ud.name) + 0);
			} else {
				playerScores.put(ud.name, 0);
			}
		}
		
		if(hasScored) {
			this.highscore.addPlayer(ud.name, playerScores.get(ud.name));
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
