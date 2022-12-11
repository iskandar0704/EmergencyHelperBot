package org.example.service;


import org.example.config.BotConfig;
import org.example.entity.UserEntity;
import org.example.enums.ProfileStatus;
import org.example.enums.UserStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    private SendMessage sendMessage = new SendMessage();
    @Autowired
    private UserService userService;



    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        UserEntity user = userService.getByChatId(message.getChatId());
        String text = message.getText();

        if(user.getStatus().equals(ProfileStatus.USER)){
            if(message.hasText()){

                if(text.equals("/start") && user.getStep().equals(UserStep.NEW)){
                    userStartHandler(user);
                }else if(text.equals("/menu")){
                    menuButton(user);
                }else if(text.equals("Start Register") && user.getStep().equals(UserStep.REGISTER_BUTTON)){
                    startRegisterHandler(user);
                }else if(user.getStep().equals(UserStep.NAME)){
                    nameHandler(user, text);
                }else if(user.getStep().equals(UserStep.SURNAME)){
                    surnameHandler(user,text);
                }else if(user.getStep().equals(UserStep.MENU)){
                    if(text.equals("Emergency call")){
                        emergencyCallMenu(user);
                    }else if(text.equals("My location")){
                        getMyLocation(user);
                        menuButton(user);
                    }else if(text.equals("Personal data")){
                        personalDataButton(user);
                        menuButton(user);
                    }else if(text.equals("Edit personal data")){
                        startRegisterHandler(user);
                    }else {
                        sendMessage.setChatId(user.getChatId());
                        sendMessage.setText("Wrong action!");
                        send(sendMessage);
                    }
                }else if(user.getStep().equals(UserStep.CALL_EMERGENCY_BUTTON)){
                    if(text.equals("Fire station")){
                        fireStationButton(user);
                    }else if(text.equals("Police station")){
                        policeStationButton(user);
                    }else if(text.equals("Ambulance")){
                        ambulanceStationButton(user);
                    }else if(text.equals("Back")){
                        menuButton(user);
                    }
                }else if(text.equals("/start")){
                    menuButton(user);
                }else if(text.equals("No, cancel")){
                    user.setStep(UserStep.NO_SURE);
                    menuButton(user);
                }else if(text.equals("Sure!")){
                    sureHandler(user);
                }


            }else if(message.hasContact() && user.getStep().equals(UserStep.PHONE)){
                Contact contact =  update.getMessage().getContact();
                contactHandler(user,contact);
            } else if(message.hasLocation() && user.getStep().equals(UserStep.LOCATION)){
                Location location = message.getLocation();
                locationHandler(user,location);
                menuButton(user);
            }

            System.out.println(user.getName()+ "    " + user.getStep());
        }else {
            if(message.hasText()){
                if(text.equals("/start")){
                    workerStartHandler(user);
                }
            }
        }






    }


    ////////////////////// User

    private void userStartHandler(UserEntity user){
        sendMessage.setText("Good time of the day! This bot helps you to emergency call. Press register to start registering!");
        sendMessage.setChatId(user.getChatId());

        List<String> stringList = new LinkedList<>();
        stringList.add("Start Register");
        sendMessage.setReplyMarkup(createReplyButton(stringList));

        user.setStep(UserStep.REGISTER_BUTTON);
        saveUser(user);
        send(sendMessage);
    }

    private void startRegisterHandler(UserEntity user){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        sendMessage.setText("Enter your name");
        sendMessage.setChatId(user.getChatId());
        user.setStep(UserStep.NAME);
        userService.save(user);
        send(sendMessage);
    }

    private void nameHandler(UserEntity user, String name){
        user.setName(name);

        sendMessage.setText("Enter your surname");
        sendMessage.setChatId(user.getChatId());
        user.setStep(UserStep.SURNAME);
        userService.save(user);
        send(sendMessage);
    }

    private void surnameHandler(UserEntity user,String surname){
        user.setSurname(surname);
        user.setStep(UserStep.PHONE);
        userService.save(user);


        sendMessage.setText("Press button!");

        // create keyboard
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        // new list
        List<KeyboardRow> keyboard = new ArrayList<>();

        // first keyboard line
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        KeyboardButton keyboardButton = new KeyboardButton();

        keyboardButton.setText("Share your number >");
        keyboardButton.setRequestContact(true);
        keyboardFirstRow.add(keyboardButton);

        // add array to list
        keyboard.add(keyboardFirstRow);

        // add list to our keyboard
        replyKeyboardMarkup.setKeyboard(keyboard);

        send(sendMessage);
    }

    private void contactHandler(UserEntity user, Contact contact){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        String phone =  contact.getPhoneNumber();
        user.setPhone(phone);
        user.setStep(UserStep.LOCATION);
        userService.save(user);

        // create keyboard
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        // new list
        List<KeyboardRow> keyboard = new ArrayList<>();

        // first keyboard line
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        KeyboardButton keyboardButton = new KeyboardButton();

        keyboardButton.setText("Share your location >");
        keyboardButton.setRequestLocation(true);
        keyboardFirstRow.add(keyboardButton);

        // add array to list
        keyboard.add(keyboardFirstRow);

        // add list to our keyboard
        replyKeyboardMarkup.setKeyboard(keyboard);

        sendMessage.setChatId(user.getChatId());
        sendMessage.setText("Press button!");
        send(sendMessage);
    }

    private void locationHandler(UserEntity user, Location location){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        user.setLocation(location);

        sendMessage.setText("Registration finished");
        sendMessage.setChatId(user.getChatId());

        user.setStep(UserStep.FINISHED);
        userService.save(user);
        send(sendMessage);
    }

    private void menuButton(UserEntity user){
        List<String> stringList = new LinkedList<>();
        stringList.add("Emergency call");
        stringList.add("My location");
        stringList.add("Personal data");
        stringList.add("Edit personal data");

        ReplyKeyboardMarkup replyKeyboardMarkup = createReplyButton(stringList);

        sendMessage.setChatId(user.getChatId());
        sendMessage.setText("Choose action!");
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        user.setStep(UserStep.MENU);
        saveUser(user);

        send(sendMessage);
    }

    private void getMyLocation(UserEntity user){
        SendLocation sendLocation = new SendLocation();

        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendLocation.setReplyMarkup(remove);

        sendLocation.setChatId(user.getChatId());

        Location location = user.getLocation();

        sendLocation.setHeading(location.getHeading());
        sendLocation.setLatitude(location.getLatitude());
        sendLocation.setLongitude(location.getLongitude());
        sendLocation.setLivePeriod(location.getLivePeriod());
        sendLocation.setHorizontalAccuracy(location.getHorizontalAccuracy());
        sendLocation.setProximityAlertRadius(location.getProximityAlertRadius());

        user.setStep(UserStep.MENU);
        saveUser(user);

        send(sendLocation);
    }

    private void emergencyCallMenu(UserEntity user){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        List<String> stringList = new LinkedList<>();
        stringList.add("Fire station");
        stringList.add("Police station");
        stringList.add("Ambulance");
        stringList.add("Back");

        ReplyKeyboardMarkup replyKeyboardMarkup = createReplyButton(stringList);

        sendMessage.setChatId(user.getChatId());
        sendMessage.setText("Choose emergency type!");
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        user.setStep(UserStep.CALL_EMERGENCY_BUTTON);
        saveUser(user);

        send(sendMessage);
    }

    private void personalDataButton(UserEntity user){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        String text = "Short info \n"
                 + user.getName() + "\n"
                + user.getSurname() + "\n"
                + user.getPhone() + "\n"
                +user.getStatus().toString() + "\n"
                +user.getCreatedDate();


        user.setStep(UserStep.PERSONAL_DATA);
        saveUser(user);

        sendMessage.setText(text);
        sendMessage.setChatId(user.getChatId());
        send(sendMessage);
    }

    private void fireStationButton(UserEntity user){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        List<String> stringList = new LinkedList<>();
        stringList.add("Sure!");
        stringList.add("No, cancel");

        ReplyKeyboardMarkup replyKeyboardMarkup = createReplyButton(stringList);

        sendMessage.setChatId(user.getChatId());
        sendMessage.setText("Are you sure of calling fire station?");
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        user.setStep(UserStep.FIREMAN_BUTTON);
        saveUser(user);

        send(sendMessage);
    }

    private void policeStationButton(UserEntity user){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        List<String> stringList = new LinkedList<>();
        stringList.add("Sure!");
        stringList.add("No, cancel");

        ReplyKeyboardMarkup replyKeyboardMarkup = createReplyButton(stringList);

        sendMessage.setChatId(user.getChatId());
        sendMessage.setText("Are you sure of calling police station?");
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        user.setStep(UserStep.POLICE_BUTTON);
        saveUser(user);

        send(sendMessage);
    }

    private void ambulanceStationButton(UserEntity user){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        List<String> stringList = new LinkedList<>();
        stringList.add("Sure!");
        stringList.add("No, cancel");

        ReplyKeyboardMarkup replyKeyboardMarkup = createReplyButton(stringList);

        sendMessage.setChatId(user.getChatId());
        sendMessage.setText("Are you sure of calling ambulance station?");
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        user.setStep(UserStep.AMBULANCE_BUTTON);
        saveUser(user);

        send(sendMessage);
    }

    private void sureHandler(UserEntity user){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        ProfileStatus status = getStatusByStep(user.getStep());
        Long chatId = userService.getChatIdByStatus(status);

        sendMessageToWorker(chatId,user);
        sendLocationToWorker(chatId,user);

        sendMessage.setChatId(user.getChatId());
        sendMessage.setText("Your call registered. Emergency workers will come near time.");

        user.setStep(UserStep.SURE);
        saveUser(user);

        send(sendMessage);
    }

    private ProfileStatus getStatusByStep(UserStep step){
        if(step.equals(UserStep.POLICE_BUTTON)){
            return ProfileStatus.POLICE_FREE;
        }else if(step.equals(UserStep.FIREMAN_BUTTON)){
            return ProfileStatus.FIRE_FREE;
        }else if(step.equals(UserStep.AMBULANCE_BUTTON)){
            return ProfileStatus.AMBULANCE_FREE;
        }
        return null;
    }


    //////////////////////////

    //////////////////////// STATION

    private void workerStartHandler(UserEntity user){
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(remove);

        sendMessage.setText("Wait for calls");
        sendMessage.setChatId(user.getChatId());

        send(sendMessage);
    }

    private void sendMessageToWorker(Long chatId, UserEntity user){
        SendMessage sendMessage1 = new SendMessage();

        String text = "Emergency info \n"
                + user.getName() + "\n"
                + user.getSurname() + "\n"
                + user.getPhone() + "\n"
                +user.getStatus().toString() + "\n"
                +user.getCreatedDate();

        sendMessage1.setChatId(chatId);
        sendMessage1.setText(text);

        send(sendMessage1);
    }

    private void sendLocationToWorker(Long chatId, UserEntity user){
        SendLocation sendLocation = new SendLocation();

        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        sendLocation.setReplyMarkup(remove);

        sendLocation.setChatId(chatId);

        Location location = user.getLocation();

        sendLocation.setHeading(location.getHeading());
        sendLocation.setLatitude(location.getLatitude());
        sendLocation.setLongitude(location.getLongitude());
        sendLocation.setLivePeriod(location.getLivePeriod());
        sendLocation.setHorizontalAccuracy(location.getHorizontalAccuracy());
        sendLocation.setProximityAlertRadius(location.getProximityAlertRadius());

        send(sendLocation);
    }

    /////////////////////////



    public void saveUser(UserEntity user){
        userService.save(user);
    }

    public void send(SendMessage sendMessage){

        try {
            execute(sendMessage);
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    public void send(SendLocation sendLocation){

        try {
            execute(sendLocation);
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }



    public ReplyKeyboardMarkup createReplyButton(List<String> buttonList){
        List<KeyboardRow> rowList = new LinkedList<>();
        for (int i = 0; i < buttonList.size(); i++) {
            KeyboardRow row = new KeyboardRow();
            row.add(buttonList.get(i));
            rowList.add(row);
        }

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(rowList);

        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        return replyKeyboardMarkup;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
    @Override
    public String getBotToken() {
        return config.getToken();
    }
}
