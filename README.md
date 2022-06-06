![symbol](https://user-images.githubusercontent.com/90979711/150548720-12608103-c91f-4500-b592-a6f6e2fb846f.jpg) 
* avatar picture of Reminder bot

## Description

LongPull telegram bot what implementation reminders fucntion. It's using Spring Boot, lombok library, PostgreSQL database, and Hibernate ORM.
Work of this bot demonstrates next GIF-animation 

![bot](https://user-images.githubusercontent.com/90979711/163603337-b7e23c7d-095f-4322-80ec-0b5ec6cf1a5f.gif)


Users' input maybye not right. To correct work it's implementing input's validate and send appropriate response . Example:

![botGif2](https://user-images.githubusercontent.com/90979711/158227397-93380217-99e6-4abd-95eb-e2326dfcbe3e.gif)
 
## Build and run
On your machine must be installed Postgres with next database scheme

CREATE TABLE DETAILS(id int GENERATED BY DEFAULT AS IDENTITY primary key,
KEY_TO_DECRYPT character varying (10),
TIME_TO_SEND boolean,
LAST_SEND_TIME character varying(7), COUNT_SEND_OF_REMIND int);

CREATE TABLE USERS(id int GENERATED BY DEFAULT AS IDENTITY primary key, CHAT_ID text,
IS_ACTIVE boolean, IS_STARTED boolean);

CREATE TABLE REMINDS(id int GENERATED BY DEFAULT AS IDENTITY primary key,
ENCRYPT_MAINTENANCE text, REMIND_DATE CHARACTER VARYING(13),
details_id int, user_id int,
foreign key(details_id) references details(id) ON DELETE CASCADE,
foreign key(user_id) references users(id) ON DELETE CASCADE);


Pull that project. Initialize bot.username and bot.token in application.properties by self enviromentes variables, config hibernate configuration file and after start it
