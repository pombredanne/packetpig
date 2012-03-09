package com.packetloop.packetpig.loaders.pcap.file;

import com.packetloop.packetpig.loaders.pcap.conversation.ConversationRecordReader;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.pig.data.TupleFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConversationFileRecordReader extends ConversationRecordReader {
    private String fileDumpPath;
    private boolean shouldDump;
    private FileSystem fs;
    private String filter;

    public ConversationFileRecordReader(String pathToTcp, String fileDumpPath, String filter) {
        super(pathToTcp);
        this.fileDumpPath = fileDumpPath;
        this.filter = filter;
    }

    @Override
    public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
        super.initialize(split, context);

        fs = FileSystem.get(context.getConfiguration());
        shouldDump = fileDumpPath != null && fileDumpPath.isEmpty() == false;

        String cmd = pathToTcp + " -of tsv -om http_body -r " + path;

        if (filter != null)
            cmd += " -i " + filter;

        if (shouldDump)
            cmd += " -xf";

        ProcessBuilder builder = new ProcessBuilder(cmd.split(" "));
        Process process = builder.start();

        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        String line = reader.readLine();
        if (line == null)
            return false;

        tuple = TupleFactory.getInstance().newTuple();

        String[] bits = line.split("\t");

        Float f = new Float(bits[0]);
        key = f.longValue();

        for (int i = 1; i < 12; i++)
            tuple.append(bits[i]);

        if (shouldDump) {
            String tempFile = bits[12];
            String dst = bits[10] + bits[7];
            fs.moveFromLocalFile(new Path(tempFile), new Path(fileDumpPath, dst));
        }

        return true;
    }
}
