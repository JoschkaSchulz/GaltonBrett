/*
 * @name: TestPinata
 * @author: Carola Christoffel & Joschka Schulz
 * @description:
 * 		Unser kleines Spiel für das Reperbahnfestival.
 * 
 * @changelog:
 * 		(noch nicht im Blog) 23.05.2013:
 * 			- Twitter Reader wird nun innerhalb eines Threads ausgeführt
 * 			- Bug mit Gravity nach Zero Gravtiy gefixed (Antistuck)
 * 			- Skalierbarkeit
 * 			- Highscore
 * 			- Leichtere Fehlerbehebung mit "Offline Modus"
 * 			- Weitere Animationen für Highscore hinzugefügt
 * 			- Änderbarer Suchfilter
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

	/**
	 * Die Klasse UserData ist dafür da, jedem Monster Werte mit 
	 * zu geben, wie z.B. das Bild aussieht.
	 */
	class UserData {
		public int image;						//Als Int Welches Monster es ist
		public String text = "Lorem Ipsum";		//Twitter Nachricht, Default: Lorem Ipsum
		public String name = "Random Guest";	//Twitter Name, Default: Random Guest
		
		public UserData() {						//Konstrucktor
		};
	}
	
	/**
	 * Die Klasse Highscore verwaltet die x besten Twitterer.
	 */
	class Highscore {
		LinkedList<Integer> points = new LinkedList<>();	//max 10 besten Punkte
		LinkedList<String> name = new LinkedList<>();		//max 10 besten Namen
		
		/**
		 * addPlayer versucht einen neuen Spieler dem Highscore hinzu-
		 * zufügen indem es kontrolliert ob er genug Punkte hat um in
		 * der Liste aufgeführt zu werden.
		 * 
		 * @param name Der Name des Spielers
		 * @param points Die Punkte des Spielers
		 */
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
		
		/**
		 * Getter für die Punkte Liste
		 * 
		 * @return Eine Liste mit den Punkten
		 */
		public LinkedList<Integer> getPoints() {
			return points;
		}
		
		/**
		 * Getter für die Namens Liste
		 * 
		 * @return Eine Liste mit den Namen
		 */
		public LinkedList<String> getName() {
			return name;
		}
	}

	/**
	 * Der MonsterTweetReader sorgt dafür das die Tweets dauernd im Hintergrund
	 * aktualisiert werden.
	 */
	class MonsterTweetReader extends Thread{
		public void run() {
			try {
				while(true) {
					try {
						TestPinata.this.twitterReader();
					}catch(Exception e ) {}
					Thread.sleep(5000);
				}
			}catch(Exception e) {}
		}
	}
	
	/**
	 * Die Klasse Tweet hält Informationen über eine Twitter Nachricht
	 */
	class Tweet {
		public String name;
		public String text;
		public void Tweet(String n, String t) {name = n; text = t;}
		public String toString() {
			return name;
		}
	}
	
	//Zählvariablen für Tweets
	private long lastTweet = 0;
	private LinkedList<Tweet> tweets;
	private boolean firstRun = true;
	
	//ID's der verschiedenen Bilder in der UserData Klasse
	private final static int BLAUES_MONSTER1 = 1;		
	private final static int PINKES_MONSTER1 = 2;
	private final static int ORANGES_MONSTER1 = 3;

	//ID'S für bestimmte Tasten
	private final static int LEERTASTE = 32;
	private final static int LINKS = 37;
	private final static int HOCH = 38;
	private final static int RECHTS = 39;
	private final static int RUNTER = 40;
	private final static int A_TASTE = 65;
	private final static int D_TASTE = 68;

	private Physics physics;	//Physik der Simulation
	private World world;		//Genaueres Physikabbild der Simulation
	
	private int crosshairX = 300;	//Pfadenkreuz X Koordinaten
	private int crosshairY = 35;	//Pfadenkreuz Y Koordinaten
	
	private PImage img, img2, img3;		//Bilder für die Monster
	private int time, throwtime;		//Zeiten für Zeitsteuerung (Highscore und Tweets)

	private HashMap<String, Integer> playerScores;	//Alle Spieler Punkte
	
	//Die Highscore
	private Highscore highscore;	//Highscore der besten X Spieler
	
	//Die Standardschriftart
	private PFont font;				//Standardschriftart der Applikation
	
	//Eigenschaften für die Fenstergröße
	public static final int SCREEN_W = 800;//1920;	//Auflösung breite
	public static final int SCREEN_H = 480;//1080;	//Auflösung höhe
	
	private String hashtag;		//Der Hashtag nachdem geschaut werden soll
	private int textHeight;	//Die Texthöhe
	
	//Für die Grafische Highsoreanzeige
	private boolean highscoreSwitch = false;	//Der wechsel ausfahren/einfahren
	private int highscoreX = 0;				//Die x Koordination von highscore
	private int highscoreY = 0;				//Die y Koordinaten von highscore
	private int highscoreswitchTime = 25000;	//Default Switch Time
	private int switchOut = 5000;				//Zeit zum ausblenden
	private int switchIn = 25000;				//Zeit zum anzeigen
	
	/**
	 * Die Methode die alles einrichtet :P
	 */
	public void setup() {
		//Setzte die größe des Fensters
		size(TestPinata.SCREEN_W, TestPinata.SCREEN_H);
		//Setzte die Zeichengeschwindigkeit auf 60 Bilder
		frameRate(60);
		//Setzte das AA auf 8x
		smooth(8);

		//Initalisiere Listen und Maps
		tweets = new LinkedList<>();
		playerScores = new HashMap<>();
		highscore = new Highscore();
		
		//Setzte den Default Hashtag
		hashtag = "Hamburg";
		
		//Setzte Zeitvariablen für den Highscore
		time = throwtime = millis();
		
		//Lade die vielen kleinen Monster :D
		img = loadImage("blaues_monster1.png");
		img.resize(width/32,width/32);//32, 32);

		img2 = loadImage("pinkes_monster1.png");
		img2.resize(width/32,width/32);
		
		img3 = loadImage("oranges_monster1.png");
		img3.resize(width/32,width/32);
		
		//Font erstellen und Texthöhe bestellen
		textHeight = width/64;
		font = createFont("Audiowide-Regular.ttf",textHeight);
		textFont(font);
		
		//highscore default
		highscoreX = (int)getPerX(101);
		
		//Erstelle die Physik und die "Welt"
		initScene();
		
		//Lese am Start einmal die Twitter Nachrichten
		twitterReader();
		
		//Fülle die Welt mit Inhalt
		createWorld();
		
		//Erstelle den Twitter Reader Thread
		MonsterTweetReader mtr = new MonsterTweetReader();
		mtr.start();	//Starte den Thread
	}
	
	/**
	 * Die main benutzten wir um:
	 * 1. Die Anwendung in einer Art Vollbildmodus zu starten
	 * 2. Eine runable .jar erzeugen
	 * 
	 * @param args Parameter werden nicht benutzt
	 */
	public static void main(String[] args) {
		Frame frame = new Frame("Regenbogencookies");	//Name des Fensters
		
		//Obere Leiste ein/ausschalten (Vollbild annähernd)
		frame.setUndecorated(true);
		
		//Binde die Applet ein
		PApplet applet = new TestPinata(   );
		frame.add(applet);
		applet.init(   );
		frame.setBounds(0, 0, TestPinata.SCREEN_W, TestPinata.SCREEN_H); 
		
		//Mache alles Sichtabr
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
	
	/**
	 * Die Methode twitterReader wird vom Thread verwendet und ganz am Anfang
	 * in der Setup einmal um die Twitternachrichten sortiert nach dem Hashtag
	 * auszulesen.
	 */
	public void twitterReader() { //PiniataCookie
		//Bieber stress test!
		String urlst = "http://search.twitter.com/search.json?q="+hashtag;
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
			//e.printStackTrace();
		}  
	}
	
	//Variablen zum Kontrollieren der Eigenschaften des Welteninhalts
	private float minG = 0.5f;		//Minimaler Rückstoß der Bumper
	private float maxG = 0.95f;	//Maximaler Rückstoß der Bumper	
	private float abstand = 6.5f;	//Abstand der Bumper
	/**
	 * Erstellt den Inhalt der Welt (Bumper und Textarea)
	 *      ____________
	 *      | o o o|~~~|
	 *      |o o o |~~~|
	 *      | o o o|~~~|
	 *      |______|___|
	 *	
	 *      ~~~~~~~~  
	 *      70%	    ~~~
	 *			    30%
	 */
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
	
	/**
	 * Erstellt eine Scene mit Physik und setzt den neuen Renderer
	 */
	public void initScene() {
		physics = new Physics(this, width, height);
		world = physics.getWorld();
	
		physics.setCustomRenderingMethod(this, "myCustomRenderer");
	}

	/**
	 * Erstellt einem Text der den Bildschirm nicht verlassen kann und auf die
	 * koordinaten x,y zeigt.
	 * 
	 * @param text der anzuzeigen Text
	 * @param x die x koordinate auf die gezeigt werden soll
	 * @param y die y koordinate auf die gezeigt werden soll
	 * @param width die Breite die der Text nicht verlassen darf
	 * @param height die Höhe die der Text nicht verlassen darf
	 */
	public void followText(String text, int x, int y, int width, int height) {
		// Ermitteln welche Rand am weitesten entfernt ist
		int abstand = 50;

		int xAchse = constrain(x - abstand, abstand, width - (int)getPerX(5));
		int yAchse = constrain(y - abstand, abstand, height - (int)getPerY(5));

		int textsize = (int)textWidth(text)+10;
		fill(255,255,255);
		line(xAchse - 5, (yAchse - textHeight)+(textHeight + 5), x, y);
		line(xAchse - 5 + textsize + 5, (yAchse - textHeight)+(textHeight + 5), x, y);
		rect(xAchse - 5, yAchse - textHeight, textsize + 5, textHeight + 5);
		fill(0);
		text(text, xAchse, yAchse);
		noFill();
	}
	
	/**
	 * Blendet die Highscore ein
	 */
	private void showHighscore() {
		if(highscoreX > getPerX(70)) highscoreX -= (int)getPerX(1f);
	}
	
	/**
	 * Blendet die Highscore aus
	 */
	private void hideHighscore() {
		if(highscoreX < getPerX(100)) highscoreX += (int)getPerX(1f);
	}
	
	/**
	 * Der Custom Renderer der anstelle der draw Methode verwendet wird
	 * 
	 * @param world eine genauere physikalischen angabe der Scene
	 */
	public void myCustomRenderer(World world) {

		// clear the background
		background(255);//0x666666)

		//Move in the highscore
		if(highscoreSwitch) {
			showHighscore();
		} else {
			hideHighscore();
		}
		
		if( ((time+highscoreswitchTime)-millis()) <= 500) {
			if(highscoreSwitch) {
				highscoreswitchTime = switchIn;
			}else {
				highscoreswitchTime = switchOut;
			}
			highscoreSwitch = !highscoreSwitch;
			
			//System.out.println("--- Highscore ---");
			//LinkedList<String> names = this.highscore.getName();
			//LinkedList<Integer> points = this.highscore.getPoints();
			
			//for(int i = 0; i < names.size(); i++) {
			//	System.out.println((i+1) + ". Platz: " + names.get(i) + " mit " + points.get(i) + " Punkten");
			//}
			time = millis();
		}
		
		if( ((throwtime+6000)-millis()) <= 500) {
			if(this.tweets.size() > 0) {
//				println(Arrays.toString(this.tweets.toArray()));
				Tweet t = this.tweets.getFirst();
				this.tweets.removeFirst();
				createRandomFromTweet(t.name, t.text, (int)random(getPerX(5), getPerX(65)), 50);
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
						
						//Format user Text
						fill(128);
						String userText = "";
						if(ud.text.length()-1 > 24) {
							userText += ud.text.substring(0, 25)+"\n";
							if(ud.text.length()-1 > 48) {
								userText += ud.text.substring(25, 48);
							} else userText += ud.text.substring(25, ud.text.length()-1);
						}else{
							userText += ud.text.substring(0, ud.text.length()-1)+"\n";
						}
							
						if(playerScores.get(ud.name) != null) {
							//Twitter Nachrichten
							text(ud.name + " ~~ Punkte: ("+playerScores.get(ud.name)+"):\n " + userText, getPerX(71), (int)pos.y);
						} else {
							//Twitter Nachrichten
							text(ud.name + " ~~ Punkte: (0):\n " + userText, getPerX(71), (int)pos.y);
						}
					}
				}
			}
		}
		
		//Nur für Demo Zwecke
		hashTagEditMode();
		
		//Highscore
		fill(255);
		rect(highscoreX,getPerY(0),getPerX(30),getPerY(100));
		fill(0);
		text("Highscore",highscoreX+5,getPerY(5));		
		try {
			LinkedList<String> names = this.highscore.getName();
			LinkedList<Integer> points = this.highscore.getPoints();
			
			for(int i = 0; i < names.size(); i++) {
				text((i+1) + ". " + names.get(i) + " mit " + points.get(i) + " P.",highscoreX+5,getPerY((i*5)+10));
			}
		}catch(Exception e) {}
	}

	/**
	 * Zeichnet ein Pfadenkreuz, nur für Testzwecke
	 */
	private void crosshair() {
		noFill();
		rect(crosshairX-5,crosshairY-5,10,10);
		line(crosshairX,crosshairY,crosshairX,crosshairY+30);
		line(crosshairX,crosshairY,crosshairX,crosshairY-30);
		line(crosshairX,crosshairY,crosshairX+30,crosshairY);
		line(crosshairX,crosshairY,crosshairX-30,crosshairY);
	}
	
	/**
	 * Erzeugt ein Zufälliges Monster dem Tweet Eigenschaften mitgegeben 
	 * werden.
	 * 
	 * @param name Twittername der Nachricht
	 * @param text Twittertext der Nachricht
	 * @param x position wo das Objekt erzeugt werden soll
	 * @param y position wo das Objekt erzeugt werden soll
	 */
	private void createRandomFromTweet(String name, String text, int x, int y) {
		UserData ud = new UserData();
		ud.name = name;
		//Textfilter
		ud.text = text.replace("\n","")
						.replace(" fuck ", "****");
		ud.image = (int) (Math.random() * 3)+1;

		physics.setDensity(1.0f);
		physics.setFriction(1.0f);
		physics.setRestitution(0.7f);
		physics.createCircle(x, y, width/64).setUserData(ud);
	}
	
	/**
	 * Erzeugt ein neues Zufälliges Monster ohne angehengte Daten
	 * 
	 * @param x	 die position an der das Monster in den Topf geworfen werden soll
	 * @param y die position an der das Monster in den Topf geworfen werden soll
	 */
	private void createRandom(int x, int y) {
		UserData ud = new UserData();
		ud.image = (int) (Math.random() * 3)+1;

		physics.setDensity(1.0f);
		physics.setFriction(1.0f);
		physics.setRestitution(0.7f);
		physics.createCircle(x, y, width/64).setUserData(ud);
	}

	/**
	 * Zeichnet die Behählter und die makierungen für die Punkte
	 */
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
	
	/**
	 * Überprüft per Kollisionsabfrage ob ein Monster in einem Behälter
	 * der Punkte enthält gefallen ist.
	 * 
	 * @param world die world von Physiks damit das Monster entfernt werden kann.
	 * @param pos die Position als Vec2 bom Behälter
	 * @param body der Body vo Monster
	 * @param ud die UserData vom Monster
	 */
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
			//points += 100;
			hasScored = true;
			if(playerScores.get(ud.name) != null) {
				playerScores.put(ud.name, playerScores.get(ud.name) + 100);
			} else {
				playerScores.put(ud.name, 100);
			}
		}else if(collision(pos, p50p1, p50s1) || collision(pos, p50p2, p50s2)) {
			world.destroyBody(body);
			//points += 50;
			hasScored = true;
			if(playerScores.get(ud.name) != null) {
				playerScores.put(ud.name, playerScores.get(ud.name) + 50);
			} else {
				playerScores.put(ud.name, 50);
			}
		}else if(collision(pos, p25p1, p25s1) || collision(pos, p25p2, p25s2)) {
			world.destroyBody(body);
			//points += 25;
			hasScored = true;
			if(playerScores.get(ud.name) != null) {
				playerScores.put(ud.name, playerScores.get(ud.name) + 25);
			} else {
				playerScores.put(ud.name, 25);
			}
		}else if(collision(pos, p0p1, p0s1) || collision(pos, p0p2, p0s2)) {
			world.destroyBody(body);
			//points += 0;
			hasScored = true;
			if(playerScores.get(ud.name) != null) {
				playerScores.put(ud.name, playerScores.get(ud.name) + 0);
			} else {
				playerScores.put(ud.name, 0);
			}
		}
		
		if(hasScored) {
			//Dieses try wird komischerweise nur im Offline Modus ausgelöst
			//Zur sicherheit falls das Internet einmal instabil ist, habe 
			//ich diesen kleinen Hack eingebaut der alles richtet... warum
			//es klappt keine Ahnung -.-
			try{
				this.highscore.addPlayer(ud.name, playerScores.get(ud.name));
			}catch(Exception e) {
				try {
					this.highscore.addPlayer(ud.name, playerScores.get(ud.name));
				}catch(Exception ex) {
					System.err.println("Böser Hack! Fehler beim Schreiben des Highscores!");	
				}
			}
		}
	}
	
	/**
	 * Simple Kollisionskontrolle
	 * 
	 * @param objPos Objekt position als Vec2
	 * @param tarPos Target position als Vec2
	 * @param tarSize Target size als Vec2
	 * @return true bei Kollision, false bei nicht Kollision
	 */
	public boolean collision(Vec2 objPos, Vec2 tarPos, Vec2 tarSize) {
		return (objPos.x > tarPos.x && objPos.x < tarPos.x + tarSize.x &&
					objPos.y > tarPos.y && objPos.y < tarPos.y + tarSize.y); 
	}
	
	/**
	 * Einfache Methode zum debuggen. Mit ihr lässt sich der Hashtag ändern.
	 */
	private boolean hashTagEditMode = false;
	private void hashTagEditMode() {
		if(hashTagEditMode) {
			fill(255,0,0);
			rect(getPerX(70),getPerY(0),getPerX(30),getPerY(5));
			fill(0);
			stroke(0);
			text("Suchfilter:"+hashtag,getPerX(71),getPerY(3));
		}else{
			fill(0);
			stroke(0);
			text("Suchfilter ändern mit \"h\"\nSuchfilter: "+hashtag,getPerX(71),getPerY(4));
		}
	}
	
	/**
	 * EventListener für den Tastendruck
	 */
	public void keyPressed() {
		if(hashTagEditMode) {
			//Nur für Demo zwecke
			if(keyCode == 10) {
				hashTagEditMode = !hashTagEditMode;
				tweets.clear();
			}else{
				if(keyCode >= 65 && keyCode <= 90) {
					hashtag += key;
				}else if(keyCode == 8){
					if(hashtag.length() > 0) 
						hashtag = hashtag.substring(0, hashtag.length()-1);
				}
			}
		}else{
			//Nur für Demo zwecke
			if(key == 'h') hashTagEditMode = !hashTagEditMode;
			
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
	}

	/**
	 * Processing draw() Methode, wird nicht mehr verwendet!
	 */
	public void draw() {
//		background(255f, 15f);
//
//		// Show the gravity
//		stroke(255, 0, 0);
//		Vec2 g = world.getGravity();
//		line(width / 2.0f, height / 2.0f, (width / 2.0f) + g.x, (height / 2.0f)
//				- g.y);
//		fill(255, 0, 0);
//		ellipse((width / 2.0f), (height / 2.0f), 4, 4);
//		
//		text("Gravity X:"+g.x+" Y:"+g.y, 605, 10);
//
	}
}
