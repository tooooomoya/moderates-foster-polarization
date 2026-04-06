package agent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class RepostLogger {
    private static PrintWriter out;

    // シミュレーション開始時に1回だけ呼ぶ
    public static void init(String folderPath) {
        try {
            File file = new File(folderPath + "repost_log.csv");
            
            // 親フォルダが存在しない場合は念のため作成しておく
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            // ★ 第二引数を false にする（あるいは書かない）ことで、既存のファイルを上書き（リセット）する
            FileWriter fw = new FileWriter(file, false);
            BufferedWriter bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);

            // 毎回ファイルを新しく作り直すため、無条件でヘッダーを書き込む
            out.println("step,original_post_id,original_author_id,reposter_id,reposter_opinion,reposter_class");
            out.flush(); // ヘッダーを即座に書き出す
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 記録用のメソッド（synchronizedをつけてスレッドセーフに）
    public static synchronized void log(int step, int origPostId, int origAuthorId, int reposterId, double reposterOp, int reposterClass) {
        if (out != null) {
            out.printf("%d,%d,%d,%d,%.4f,%d%n", step, origPostId, origAuthorId, reposterId, reposterOp, reposterClass);
        }
    }

    // シミュレーション終了時に必ず呼ぶ
    public static void close() {
        if (out != null) {
            out.flush();
            out.close();
        }
    }
}