package task;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/***
 * Разработать приложение на основе UDP-соединения, позволяющее осуществлять взаимодействие клиента и сервера
 * по совместному решению задач обработки информации.
 * Приложение должно располагать возможностью передачи и модифицирования получаемых (передаваемых) данных.
 * Возможности клиента: передать серверу исходные параметры (вводятся с клавиатуры) для расчета значения функции,
 * а также получить расчетное значение функции.
 * Возможности сервера: по полученным от клиента исходным параметрам рассчитать значение функции,
 * передать клиенту расчетное значение функции, а также сохранить исходные параметры и значение функции в файл.
 */
public class UDPServer {
    static DatagramPacket datagramPacket;
    static DatagramSocket datagramSocket = null;

    private static final String REPORT_FILE = "./result.txt";

    private static double calculateFormula(final List<Float> parameters) {
        float x = parameters.get(0);
        float y = parameters.get(1);
        float z = parameters.get(2);
        return ((2 * cos(x - Math.PI / 6)) / (Math.pow(Math.E, 0.5) + Math.pow(sin(y), 2))) *
                (1 + (Math.pow(z, 2) / (3 - Math.pow(z, 5) / 5)));
    }

    private static void sendDataToClient(String resultMessage) {
        InetAddress clientAddress = datagramPacket.getAddress();
        int clientPort = datagramPacket.getPort();

        byte[] buffer = resultMessage.getBytes();

        DatagramPacket response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
        try {
            datagramSocket.send(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateReportFile(final List<Float> parameters, final double result) {
        //откроем DATA_FILE для чтения
        RandomAccessFile reportFile = null;
        try {
            //открываем файл для записи
            reportFile = new RandomAccessFile(new File(REPORT_FILE), "rw");
            //очищаем файл
            reportFile.setLength(0);
            //записываем исходные параметы
            reportFile.writeBytes(String.format("x = %s%n", parameters.get(0)));
            reportFile.writeBytes(String.format("y = %s%n", parameters.get(1)));
            reportFile.writeBytes(String.format("z = %s%n", parameters.get(2)));
            reportFile.writeBytes("\n");

            //записываем результат
            reportFile.writeBytes(String.format("Result: %s%n", result));

            System.out.printf("Результат работы успешно записан в файл %s%n", REPORT_FILE);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Failed to write to the file");
        } finally {
            try {
                if (reportFile != null) {
                    reportFile.close();
                }
            } catch (IOException e) {
                System.out.println("Failed to close the file");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Сервер запущен....");
            datagramSocket = new DatagramSocket(3000);
            byte[] buf = new byte[1024];
            datagramPacket = new DatagramPacket(buf, 1024);

            List<Float> parameters = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                datagramSocket.receive(datagramPacket);
                //объявление строки и присваивание ей данных потока ввода, представленных
                //в виде строки (передано клиентом)
                String clientMessageReceived = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                try {
                    parameters.add(Float.parseFloat(clientMessageReceived));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                System.out.printf("Сообщение от клиента: '%s'%n", clientMessageReceived);
            }

            if (parameters.size() == 3) {
                System.out.println("Получены все параметры. Вычисляем формулу....");
                double result = calculateFormula(parameters);
                System.out.printf("Результат вычислений: %s%n", result);
                //отправляем вычисления на клиент
                sendDataToClient(String.valueOf(result));
                generateReportFile(parameters, result);
            } else {
                sendDataToClient("Ошибка ввода данных. Повторите еще раз...");
            }

        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
                System.out.println("Соедиенение с клиентом разорвано....");
            }
        }
    }
}
