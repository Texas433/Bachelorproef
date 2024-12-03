from sqlalchemy import SQLALchemy
import datetime
from flask import request,Flask,jsonify
from flask_cors import CORS
from werkzeug.security import check_password_hash


db = SQLALchemy()
app = Flask(__name__)

class User(db.Model):
    __tablename__ = "User"
    user_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    username = db.Column(db.String, unique=True, nullable=False)
    password = db.Column(db.String, nullable=False)


class sensor_data(db.Model):
    __tablename__ = "sensor_data"
    data_id = db.Column(db.Integer, primary_key=True, autoincrement =True)
    user_id = db.Column(db.Integer, db.ForeignKey('User.user_id', ondelete='SET NULL'), nullable=False)
    temperature = db.Column(db.Real)
    humidity = db.Column(db.Real)
    pressure = db.Column(db.Real)
    timestamp = db.Column(db.DateTime,default=datetime.utcnow)


database_path = '~/Bachelorproef/BAP/CODE/Webserver/API'

app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{database_path}'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db.init_app(app)

CORS(app)



@app.route('/')
def start():
    return jsonify("201")


@app.route('/login', methods='POST')  # hashlib -> hashlib.sha256(wachtwoord.encode()).hexdigest() 
def login():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password') #login handmatig toevoegen in db en pswd hash fixen en zelf ook handmatig toevoegen (geen registration)
    user = User.query.filter_by(username=username).first()
    if user and check_password_hash(user.password, password):
        return jsonify({"message": "Login successful"}), 200
    return jsonify({"message": "Invalid credentials"}), 401



@app.route("/postdata", methods='POST')
def postdata():
    #na dataretrieval decrypteren en splitsen
    data = request.get_json()
    decrypted_data = decrypt(data) 
    if decrypted_data:
        temperature, humidity, pressure = decrypted_data
        user_id = data.get('user_id')
        new_data = SensorData(humidity=humidity, pressure=pressure, temperature=temperature, user_id=user_id) #data_id?
        db.session.add(new_data)
        db.session.commit()
    return jsonify({"sent"}), 201
    

@app.route("/getdata", methods='GET') #moetnagecheckt worden
def getdata():
    latest_data = sensor_data.query.order_by(sensor_data.data_id.desc()).first()
    if latest_data:
        return jsonify({
            "temperature": latest_data.temperature,
            "humidity": latest_data.humidity,
            "pressure": latest_data.pressure,
            "timestamp": latest_data.timestamp.isoformat()
        })
    else:
        return jsonify({"No data found"}), 404
    

    def decrypt(data):
        pass #manuel zoek het uit pls, return 3 waarden, je mag ook splitsen hier of in een andere functie