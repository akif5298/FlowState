CP - 470 Personal Energy Cycle Predictor Project Charter – Personal Energy Cycle Predictor App Problem Statement 

Most people push through low-energy periods, leading to reduced efficiency and greater fatigue. 

A smarter approach is an app that predicts personal energy highs and lows, guiding users to schedule demanding tasks during peaks and restorative activities during dips. 



Project Objectives 

● Collect biometric and activity data from wearables: heart rate, ECG, sleep cycles, skin temperature optional. 



● Capture user typing speed and reaction time as measures of cognitive performance. 



● Predict personal energy timings through machine learning techniques. 



● Generate personalized productivity schedules and advice by using the predictions through LLMs. 



● Maximize productivity, decrease fatigue, and raise awareness about personal health patterns. 





Stakeholders 

● Primary Users: Knowledge workers, students, or anyone looking to maximize productivity. 



● Secondary Users: Those conscious about their health tendencies and keep a check on energy, sleep, and stress levels. 



● Project Sponsor / Instructor: Oversees project and approves the necessary technical design. 



● Development Team 

○ Bibek Chugh 

○ Kush Jain 

○ Akif Rahman 

CP - 470 Personal Energy Cycle Predictor 

○ Yusuf Muzaffar Iqbal 

○ Tharun Indrakumar 





Project Deliverables 

Android App \(Kotlin/Java, Android Studio\): 

● Google Fit or manufacturer's API to collect data from a wearable device. 



● Capture typing speed and reaction time. 



● Compiles user data securely. 



● Inform users on best times to study, sleep, workout, etc, based on the predicted energy graph for the day and optionally inform how tasks would affect energy levels after. 



UI / Visualization Module: 

● Graphs showing trends in heart rate, sleep quality, and predicted energy cycles. 



● Daily alerts and notifications for optimal task scheduling and energy inquiries to adjust graphs. 



ML Module \(TensorFlow Lite\): 

● Examine biometric and activity data. 



● Predict energy highs and lows over the day. 



LLM Module \(OpenAI GPT-4 / GPT-4-mini\): 

● Construct individual productivity schedules and suggestions. 



● Gives natural language guidance based on ML predictions. 



Optional Backend \(Firebase\): 

● Holds historical records and synchronizes them for multiple device use. 



● Carries push notifications for reminders on schedules. 





CP - 470 Personal Energy Cycle Predictor Justification for Project Approval 

Importance of Problem: 



Energy rhythms: the individual delivery of these rhythms will affect both productivity and health. These are the types of analyses: no existing applications put biometric \+ cognitive \+ ML 

\+ LLM together to create a schedule optimized for user energy levels. 





Technical Difficulties: 

● Wearable API integration for heart rate, sleep, and skin temperature, optionally. 



● Processing time-series data for prediction of energy through machine learning. 



● Automatic generation of reliable, personalized schedules with LLM guidance. 



● Development of a user-friendly Android interface with response-graphs and notification features. 



High-Quality Software Design: 

● This architecture will be modular: Data Collection → ML → LLM → UI → Backend. 



● API scaling for several wearables. 



Reliability and Usability: 

● Google Fit integration provides a very high level of accuracy and standardization in the biometrics collected. 



● ML prediction will be tested on multiple users to ascertain robustness. 



● The interface shall be designed for intuitive interaction, minimal setup, and easy installation 





# Document Outline

+ Project Charter – Personal Energy Cycle Predictor App   
	+ Problem Statement  
	+ Project Objectives  
	+ Stakeholders  
	+ Project Deliverables  
	+ Justification for Project Approval



