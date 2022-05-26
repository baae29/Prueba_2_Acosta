package deitel.com;

import android.os.Bundle;
//import android.support.wearable.activity.WearableActivity;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class MainActivityFragment extends Fragment {
    private DoodleView doodleView;
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;
    private boolean dialogOnScreen = false;

    // valor utilizado para determinar si el usuario agitó el dispositivo para borrar
    private static final int ACCELERATION_THRESHOLD = 100000;

    // utilizado para identificar la solicitud de uso de almacenamiento externo, que
    // la función de guardar imagen necesita
    private static final int SAVE_IMAGE_PERMISSION_REQUEST_CODE = 1;


    // llamado cuando se necesita crear la vista de Fragmento
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        setHasOptionsMenu(true);  // este fragmento tiene elementos de menú para mostrar

        // obtener una referencia a DoodleView
        doodleView = (DoodleView) view.findViewById(R.id.doodleView);

        // inicializar valores de aceleración
        acceleration = 0.00f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;
        return view;
    }

    // empezar a escuchar los eventos del sensor
    @Override
    public void onResume() {
        super.onResume();
        enableAccelerometerListening();// escucha el evento de agitación
    }

    // habilitar la escucha de eventos del acelerómetro
    private void enableAccelerometerListening() {
        //obtener el Sensor Manager
        SensorManager sensorManager =
                (SensorManager) getActivity().getSystemService(
                        Context.SENSOR_SERVICE);
        //registrarse para escuchar los eventos del acelerometro
        sensorManager.registerListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    // dejar de escuchar los eventos del acelerómetro
    @Override
    public void onPause() {
        super.onPause();
        disableAccelerometerListening();//deja de escuchar por temblar
    }

    // deshabilitar la escucha de eventos del acelerómetro
    private void disableAccelerometerListening() {
        // obtener el SensorManager
        SensorManager sensorManager =
                (SensorManager) getActivity().getSystemService(
                        Context.SENSOR_SERVICE);
        //dejar de escuchar los eventos del acelerometro
        sensorManager.unregisterListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    // controlador de eventos para eventos de acelerómetro
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        // use el acelerómetro para determinar si el usuario sacudió el dispositivo

        @Override
        public void onSensorChanged(SensorEvent event) {
            //asegurese de que no se muestren los dialogos
            if (!dialogOnScreen) {
                // obtener los valores x, y y z para SensorEvent
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                //guardar el valor de aceleracion anterior
                lastAcceleration = currentAcceleration;

                //calcula la aceleracion actual
                currentAcceleration = x * x + y * y + z * z;

                //calcula el cambio de aceleracion
                acceleration = currentAcceleration *
                        (currentAcceleration - lastAcceleration);

                //si la aceleracion esta por encima de cierto umbral
                if (acceleration > ACCELERATION_THRESHOLD)
                    confirmErase();
            }
        }

        //metodo requerido de interfaz SensorEventListener
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }


    };

    //confirma si la imagen debe borrarse
    private void confirmErase() {
        EraseImageDialogFragment fragment = new EraseImageDialogFragment();
        fragment.show(getFragmentManager(), "erase dialog");
    }

    // muestra los elementos del menú del fragmento
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.doodle_fragment_menu, menu);
    }

    // manejar la elección del menú de opciones
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // cambiar basado en el ID de MenuItem
        switch (item.getItemId()) {
            case R.id.color:
                ColorDialogFragment colorDialog = new ColorDialogFragment();
                colorDialog.show(getFragmentManager(), "color dialog");
                return true; // consume el evento del menú
            case R.id.line_width:
                LineWidthDialogFragment widthDialog =
                        new LineWidthDialogFragment();
                widthDialog.show(getFragmentManager(), "line width dialog");
                return true;// consume el evento del menú
            case R.id.delete_drawing:
                confirmErase();//confirmar antes de borrar la imagen
                return true;// consume el evento del menú
            case R.id.save:
                saveImage();//verifica el permiso y guardar la imagen actual
                return true;// consume el evento del menú
            case R.id.print:
                doodleView.printImage();//imprimir las imagenes actuales
                return true;// consume el evento del menú

        }
        return super.onOptionsItemSelected(item);
    }

    // solicita el permiso necesario para guardar la imagen si
    // necesario o guarda la imagen si la aplicación ya tiene permiso
    private void saveImage() {
        //comprueba si la aplicacion no tiene el permiso necesario
        //para guardar la imagen
        if (getContext().checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {

            // muestra una explicación de por qué se necesita permiso
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                //establecer el mensaje de Alert Dialog
                builder.setMessage(R.string.permission_explanation);

                //agrega un boton Aceptar al dialogo
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //pedir permiso
                                requestPermissions(new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        SAVE_IMAGE_PERMISSION_REQUEST_CODE);
                            }
                        }
                );
                //mostrat el dialogo
                builder.create().show();
            } else {
                //Peir permiso
                requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        SAVE_IMAGE_PERMISSION_REQUEST_CODE);
            }
        } else { // si la aplicacion ya tiene permiso para escribir en el almacenamiento externo
            doodleView.saveImage(); //guarda la imagen
        }

    }

    // llamado por el sistema cuando el usuario concede o niega el
    // permiso para guardar una imagen
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

        // el conmutador elige la accion adecuada segun las caracteristicas
        //permiso solicitado
        switch (requestCode) {
            case SAVE_IMAGE_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    doodleView.saveImage();//guardar la imagen
                return;
        }
    }

    //devuelve DoodleView
    public DoodleView getDoodleView() {
        return doodleView;
    }

    //indica si se muestra un dialogo
    public void setDialogOnScreen(boolean visible) {

        dialogOnScreen = visible;
    }
}
