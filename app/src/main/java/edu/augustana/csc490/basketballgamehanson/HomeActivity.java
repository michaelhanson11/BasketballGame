package edu.augustana.csc490.basketballgamehanson;

/**
 * Created by MHanson on 4/6/2015.
 */
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by MHanson on 4/5/2015.
 */
public class HomeActivity extends Activity {
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);

        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startIntent = new Intent(view.getContext(), MainActivity.class);
                startActivityForResult(startIntent, 0);
            }
        });
    }

}
