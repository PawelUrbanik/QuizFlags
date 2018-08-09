package com.example.pawel.quizflags;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    /* Znacznik używany przy zapisie błędów w Log */
    private static final String TAG ="QuizWithFlags Activity";

    /* Liczba flag w quizie  */
    private static final int FLAGS_IN_QUIZ = 10;

    /* Nazwy plików z obrazami flag */
    private List<String> fileNameList;

    /* Lista plików z obrazami flag biorących udział w quizie */
    private List<String> quizCountriesList;

    /* Wybrane obszary biorące udziałw quzie  */
    private Set<String> regionSet;

    /* poprawna nazwa kraju przypisana do bieżacej flagi */
    private String correctAnswer;

    /* całkowita liczba odpowiedzi */
    private int totalGuesses;

    /* Liczba poprawnych odpowiedzi */
    private int correctAnswers;

    /* Liczba wierszy przycisków odpowiedzi wyświetlanych na ekranie*/
    private int guessRows;

    /* Obiekt do losowania */
    private SecureRandom random;

    /* Obiekt używany do opóźniania procesu ładowania kolejnej flagi */
    private Handler handler;

    /* Animacja błędnej odpowiedzi */
    private Animation shakeAnimation;

    /* Głóny rozkład aplikacji */
    private LinearLayout quizLinearLayout;

    /* Widok wyświetlający numer pytania quizu  */
    private TextView questionNumberTextView;

    /* Widok wyświetlający bieżącą flage */
    private ImageView flagImageView;

    /* Tablica z wierszami przycisków odpowiedzi */
    private  LinearLayout[] guessLinearLayouts;

    /* Widok wyświetlający poprawną odpowiedz w quizie */
    private TextView answerTextView;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /* Zainnicjowanie GUI dla fragmentu */
        super.onCreateView(inflater, container, savedInstanceState);

        /* Pobranie rozkłądu dla fragmentu */
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        /* Inicjalizacja wtbranych pól */
        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        /* Inicjalizacja animacji */
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        // Inicjalizacja komponentó GUI
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);

        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        /* Configuracja nasłuchiwania zdarzeń w przyciskach odpowiedzi */
        for (LinearLayout row: guessLinearLayouts)
        {
            for (int column =0; column < row.getChildCount(); column++)
            {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListner);
            }
        }

        /* Wyświetlenie formatowanego tekstu */
        questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));

        /* Zwróć widok fragmentu do yświetlenia */
        return view;
    }

    public void updateGuessRows(SharedPreferences sharedPreferences)
    {
        /* Pobranie informacji o ilości przycisków do wyświetlenia */
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
        /* Liczba wierszy z przyciskami odpowiedzi do wyświetlenia */
        guessRows = Integer.parseInt(choices) /2;
        System.out.println(guessRows);

        /* Ukrycie wszystkich wierszy z przyciskami */
        for (LinearLayout layout: guessLinearLayouts)
        {
            layout.setVisibility(View.GONE);
        }

        /* Wyświetlenia określonej liczby wierszy z przyciskami odpowiedzi*/
        for (int row=0; row <guessRows; row++)
        {
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
        }
    }

    public void updateRegions(SharedPreferences sharedPreferences)
    {
        /* Pobranie informacji na temat wybranych przez użytkowniaka obszarów */
        regionSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    public void resetQuiz()
    {
        /* Uzyskaj dostęp do folderu asets */
        AssetManager assets = getActivity().getAssets();

        /*  Wysczyśc listę z nazwami flag*/
        fileNameList.clear();

        /* Pobierz nazwy plików obrazów flag z wybranych przez usera obszarów */
        try {
            /*Pętla  przechodząca przez każdy obszar - każdy folder */
            for (String region: regionSet){
                /* Pobranie nazw wszystkich plików znajdujących się w folderze */
                String[] paths = assets.list(region);

                /* Usunięcie z nazw plików ich rozszerzenia formatów */
                for (String path: paths)
                {
                    fileNameList.add(path.replace(".png",""));
                }
            }
        }catch (IOException e)
        {
            Log.e("TAG", "Błąd podczas ładowania plików z obrazami flag", e);
        }

        /* Resetowanie liczby poprawnych i wszystkich odpowiedzi */
        correctAnswers =0;
        totalGuesses =0;

        /* Wyczyszczenie listy krajów */
        quizCountriesList.clear();

        /* Inicjalizacja zmiennych wykorzystywanych przy losowaniu flagi */
        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        /* losowanie flag */
        while (flagCounter <= FLAGS_IN_QUIZ)
        {
            /* Wybierz losowa wartość z zakresu od 0 do liczby flag biorących udział w quzie */
            int randomIndex = random.nextInt(numberOfFlags);

            /*  Pobierz nazwę pliku o wylosowanych indexie*/
            String fileName = fileNameList.get(randomIndex);

            /* Jeżeli plik o tej nazwie nie został jeszcze wylosowany to dodaj go do listy  wybranych krajów */
            if (!quizCountriesList.contains(fileName))
            {
                quizCountriesList.add(fileName);
                ++flagCounter;
            }

        }

        /* Załaduj flagę  */
        loadNextFlag();
    }

    private void loadNextFlag()
    {
        /* Pobranie nazwy pliku bieżącej flagi */
        String nextImage = quizCountriesList.remove(0);

        /* Zaktualizowanie poprawnej odpowiedzi */
        correctAnswer = nextImage;

        /* Wyczyszczenie widoku textView */
        answerTextView.setText("");

        /* Wyświetlenie numeru bieżacego pytania */
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers+1), FLAGS_IN_QUIZ));

        /* Pobieranie nazwy obszaru bieżącej flagi */
        String region = nextImage.substring(0, nextImage.indexOf("-"));

        /* Dostęp do folderu assets */
        AssetManager assetManager = getActivity().getAssets();

        /* Otworzenie, załadowanie oraz obsadzenie obrazu flagi w widoku ImageView */
        try(InputStream inputStreamFlag = assetManager.open(region+"/"+nextImage+".png")) {

            /* Załadowanie obrazu flagi jako obiekt Drawable */
            Drawable drawableFlag = Drawable.createFromStream(inputStreamFlag,nextImage);

            /* Obsadzenie obiektu Drawable w Widoku ImageView */
            flagImageView.setImageDrawable(drawableFlag);

            /* Animacja wejscia flagi na ekran */
            animate(false);


        } catch (IOException ex)
        {
            Log.e(TAG, "Błąd podczas ładowania" +nextImage);
        }

        /* Przetasowanie nazw plików  */
        Collections.shuffle(fileNameList);

        /* Umieszcczenie prawidłowej odpowiedzi na koncu listy*/
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        /* Dodanie tekstów do przycisków odpowiedzi*/
        for (int row=0; row < guessRows; row++)
        {
            for (int column=0; column<2; column++)
            {
                /* Dostęp do przycisku i zmiana na enabled */
                Button guessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                guessButton.setVisibility(View.VISIBLE);
                guessButton.setEnabled(true);

                /* Pobranie nazwy kraju i ustawienie jej w przycisku button */
                String fileName = fileNameList.get((row*2)+ column);
                guessButton.setText(getCountryName(fileName));


            }
        }

        /* Dodanie poprawnej odpowiedzi do losowo wybranego przyciku */
        int row = random.nextInt(guessRows);
        int column = random.nextInt(2);
        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    private String getCountryName(String name){
        return name.substring(name.indexOf("-")+1).replace("_"," ");
    }

    private void animate(boolean animateOut)
    {
        /* Brak wyświetlania animiacji przy pierwszej fladze */
        if (correctAnswers==0) return;

        /* Obliczenie współrzędnych środka rozkładu */
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) /2;
        int centerY =(quizLinearLayout.getTop() + quizLinearLayout.getBottom()) /2;

        /* Obliczenie promienia animacji */
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        /* Zdefiniowanie obiektua animacji */
        Animator animator;

        /* Wariant animacji zakrywającej flagę */
        if (animateOut)
        {
            /* Utworzenie animacji  */
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX,centerY, radius, 0);

            /* Gdy animacja się skonczy  */
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadNextFlag();
                }
            });
        }
        /* Wariant animacji odkrywającej flagę  */
        else {
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX,centerY, 0, radius);
        }

        /* Określenie czasu trwania animacji */
        animator.setDuration(500);

        /* Uruchommienie am=plikacji  */
        animator.start();
    }

    private View.OnClickListener guessButtonListner  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            /* Pobranie naciśniętego przycisku oraz wyświetlanego przez niego tekstu */
            Button guessButton = (Button) v;
            String guess = guessButton.getText().toString();
            String answer =  getCountryName(correctAnswer);

            /* Inkrementracja liczby odpowiedzi udzielonych przez usera */
            ++totalGuesses;

            /* Jeżeli udzielona odpowiedź jest poprawna */
            if (guess.equals(answer))
            {
                /* Inkrementacja liczby poprawnych odpowiedzi */
                ++correctAnswers;

                /* Wyświetlenie informacji zwrotnej dla użytkownika o poprawnej odpowiedzi */
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));

                /* Dezaktywacja wszystkich przycisków odpowiedzi */
                disableButtons();

                /* Czy użytkownik udzielił odpowiedzi na wszystkie pytania */
                if (correctAnswers == FLAGS_IN_QUIZ)
                {
                    /* Tworzenie obiektu AletDialog z własnym tekstem oraz przyciskiem */
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Quiz results");
                    builder.setMessage(getString(R.string.results, totalGuesses, (1000/ (double) totalGuesses)));
                    builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resetQuiz();
                        }
                    });

                    builder.setCancelable(false);
                    builder.show();
                }
                /* Jeżeli użytkownik nie udzielił odpowiedzi na wszystkie pytania */
                else {
                    /* Odczekanie 2 sekund  i załaduj flagę*/
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animate(true);
                        }
                    }, 2000);

                }
            }
            /* Jeżeli udzielona odpowiedź nie jest poprawna */
            else {
              /* Odtworzenie animacji trzęsącej się flagi */
              flagImageView.startAnimation(shakeAnimation);

              /* Wyświetlenie informacji zwrotnej dla użytkownika o błędnej odpowiedzi */
              answerTextView.setText(R.string.incorrect_answer);
              answerTextView.setTextColor(getResources().getColor(R.color.incorect_answer, getContext().getTheme()));

              /* Dezaktywacja przycisku z błędną odpowiedza  */
              guessButton.setEnabled(false);

            }
        }
    };

        private void disableButtons()
        {
            for (int row = 0; row < guessRows; row++)
            {
                LinearLayout linearLayout = guessLinearLayouts[row];
                for (int column = 0; column<2; column++)
                {
                    Button guessButton = (Button) linearLayout.getChildAt(column);
                    guessButton.setEnabled(false);
                }
            }
        }
}
