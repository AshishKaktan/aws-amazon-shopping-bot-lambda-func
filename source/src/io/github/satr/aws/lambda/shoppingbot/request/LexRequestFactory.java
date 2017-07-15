package io.github.satr.aws.lambda.shoppingbot.request;
// Copyright © 2017, github.com/satr, MIT License

import io.github.satr.aws.lambda.shoppingbot.intents.BakeryDepartmentIntent;
import io.github.satr.aws.lambda.shoppingbot.intents.GreetingsIntent;
import io.github.satr.aws.lambda.shoppingbot.intents.MilkDepartmentIntent;
import io.github.satr.aws.lambda.shoppingbot.intents.VegetableDepartmentIntent;
import io.github.satr.aws.lambda.shoppingbot.request.strategies.intentloading.*;

import java.util.HashMap;
import java.util.Map;

public class LexRequestFactory {


    private final static Map<String, IntentLoaderStrategy> intentLoaderStrategies = new HashMap<>();
    private final static IntentLoaderStrategy unsupportedIntentLoaderStrategy = new UnsupportedIntentLoaderStrategy();

    static {
        intentLoaderStrategies.put(GreetingsIntent.Name, new GreetingsIntentLoadingStrategy());
        intentLoaderStrategies.put(BakeryDepartmentIntent.Name, new BakeryDepartmentIntentLoadingStrategy());
        intentLoaderStrategies.put(MilkDepartmentIntent.Name, new MilkDepartmentIntentLoadingStrategy());
        intentLoaderStrategies.put(VegetableDepartmentIntent.Name, new VegetableDepartmentIntentLoadingStrategy());
    }

    public static LexRequest createFromMap(Map<String, Object> input) {
        LexRequest request = new LexRequest();
        if(input == null)
            return request;

        loadMainAttributes(input, request);
        loadBotName(input, request);
        loadSessionAttributes(input, request);

        Map<String, Object> currentIntent = loadCurrentIntent(input);
        if (currentIntent != null)
            loadIntentParameters(currentIntent, request);

        return request;
    }

    private static void loadMainAttributes(Map<String, Object> input, LexRequest request) {
        loadUserId(input, request);
        request.setInputTranscript((String) input.get(LexRequestAttribute.InputTranscript));
        request.setInvocationSource(getInvocationSource(input));
        request.setOutputDialogMode(getOutputDialogMode(input));
    }

    private static void loadUserId(Map<String, Object> input, LexRequest request) {
        String userId = (String) input.get(LexRequestAttribute.UserId);
        request.setUserId(userId);
        if(userId == null)
            request.setUserIdType(UserIdType.Undefined);
        else if(userId.matches("ˆ\\d{16}$"))
            request.setUserIdType(UserIdType.Facebook);
        else
            request.setUserIdType(UserIdType.Undefined);
    }

    private static OutputDialogMode getOutputDialogMode(Map<String, Object> input) {
        return LexRequestAttribute.OutputDialogModeValue.Voice.equals((String) input.get(LexRequestAttribute.OutputDialogMode))
                ? OutputDialogMode.Voice : OutputDialogMode.Text;
    }

    private static InvocationSource getInvocationSource(Map<String, Object> input) {
        return LexRequestAttribute.InvocationSourceValue.DialogCodeHook.equals((String) input.get(LexRequestAttribute.InvocationSource))
                                        ? InvocationSource.DialogCodeHook : InvocationSource.FulfillmentCodeHook;
    }

    private static void loadSessionAttributes(Map<String, Object> input, LexRequest request) {
        Map<String, Object> sessionAttrs = (Map<String, Object>) input.get(LexRequestAttribute.SessionAttributes);
        if (sessionAttrs != null)
            request.setSessionAttributes(sessionAttrs);
    }

    private static void loadIntentParameters(Map<String, Object> currentIntent, LexRequest request) {
        request.setConfirmationStatus(getConfirmationStatus(currentIntent));
        request.setIntentName((String) currentIntent.get(LexRequestAttribute.CurrentIntentName));

        loadIntentSlots(currentIntent, request);
    }

    private static ConfirmationStatus getConfirmationStatus(Map<String, Object> currentIntent) {
        String confirmationStatus = (String) currentIntent.get(LexRequestAttribute.InvocationSource);
        return LexRequestAttribute.ConfirmationStatusValue.Confirmed.equals(confirmationStatus)
                ? ConfirmationStatus.Confirmed
                : LexRequestAttribute.ConfirmationStatusValue.Denied.equals(confirmationStatus)
                    ? ConfirmationStatus.Denied
                    : ConfirmationStatus.None;
    }

    private static Map<String, Object> loadCurrentIntent(Map<String, Object> input) {
        return (Map<String, Object>) input.get(LexRequestAttribute.CurrentIntent);
    }

    private static void loadBotName(Map<String, Object> input, LexRequest request) {
        Map<String, Object> bot = (Map<String, Object>) input.get(LexRequestAttribute.Bot);
        if (bot != null)
            request.setBotName((String) bot.get(LexRequestAttribute.BotName));
    }

    private static void loadIntentSlots(Map<String, Object> currentIntent, LexRequest request) {
        IntentLoaderStrategy strategy = getIntentLoadingStrategyBy(request.getIntentName());
        Map<String, Object> slots = (Map<String, Object>) currentIntent.get(LexRequestAttribute.Slots);
        strategy.load(request, slots);
    }

    private static IntentLoaderStrategy getIntentLoadingStrategyBy(String intentName) {
        return intentLoaderStrategies.containsKey(intentName)
                ? intentLoaderStrategies.get(intentName)
                : unsupportedIntentLoaderStrategy;
    }

}
