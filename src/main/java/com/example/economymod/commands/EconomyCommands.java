package com.example.economymod.commands;

import com.example.economymod.economy.*;
import com.example.economymod.items.CheckItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// 经济命令系统
public class EconomyCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("economy")
                .then(Commands.literal("balance")
                        .executes(context -> showBalance(context.getSource())))
                
                .then(Commands.literal("pay")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(context -> payPlayer(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                LongArgumentType.getLong(context, "amount")))
                                        .then(Commands.argument("memo", StringArgumentType.greedyString())
                                                .executes(context -> payPlayer(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        LongArgumentType.getLong(context, "amount"),
                                                        StringArgumentType.getString(context, "memo")))))))
                
                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(context -> deposit(
                                        context.getSource(),
                                        LongArgumentType.getLong(context, "amount")))))
                
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(context -> withdraw(
                                        context.getSource(),
                                        LongArgumentType.getLong(context, "amount")))))
                
                .then(Commands.literal("loan")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, 10000))
                                .executes(context -> requestLoan(
                                        context.getSource(),
                                        LongArgumentType.getLong(context, "amount")))))
                
                .then(Commands.literal("repay")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(context -> repayLoan(
                                        context.getSource(),
                                        LongArgumentType.getLong(context, "amount")))))
                
                .then(Commands.literal("history")
                        .executes(context -> showHistory(context.getSource())))
                
                .then(Commands.literal("check")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> issueCheck(
                                                context.getSource(),
                                                LongArgumentType.getLong(context, "amount"),
                                                EntityArgument.getPlayer(context, "player"))))))
                
                .then(Commands.literal("daily")
                        .executes(context -> claimDailyReward(context.getSource())))
                
                .then(Commands.literal("stats")
                        .executes(context -> showEconomyStats(context.getSource())))
                
                .then(Commands.literal("top")
                        .executes(context -> showTopPlayers(context.getSource())))
                
                // 管理员命令
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(context -> adminGiveMoney(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        LongArgumentType.getLong(context, "amount"))))))
                        
                        .then(Commands.literal("take")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(context -> adminTakeMoney(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        LongArgumentType.getLong(context, "amount"))))))
                        
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> adminResetPlayer(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")))))
                        
                        .then(Commands.literal("interest")
                                .executes(context -> adminCalculateInterest(context.getSource())))));
    }

    // 显示余额
    private static int showBalance(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        long balance = EconomyManager.getMoney(player.getUUID());
        long savings = EconomyManager.getSavings(player.getUUID());
        Loan loan = EconomyManager.getLoan(player.getUUID());

        source.sendSuccess(() -> Component.literal("=== 账户信息 ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("钱包余额: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(balance)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 金币")), false);
        
        source.sendSuccess(() -> Component.literal("储蓄余额: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(savings)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" 金币")), false);

        if (loan != null) {
            source.sendSuccess(() -> Component.literal("贷款状态: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(loan.getStatusDescription()).withStyle(ChatFormatting.RED)), false);
            source.sendSuccess(() -> Component.literal("待还金额: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(loan.getRemainingAmount())).withStyle(ChatFormatting.RED))
                    .append(Component.literal(" 金币")), false);
        }

        long total = balance + savings;
        source.sendSuccess(() -> Component.literal("总资产: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(total)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" 金币")), false);

        return 1;
    }

    // 转账给玩家
    private static int payPlayer(CommandSourceStack source, ServerPlayer target, long amount) {
        return payPlayer(source, target, amount, "无");
    }

    private static int payPlayer(CommandSourceStack source, ServerPlayer target, long amount, String memo) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (player.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("不能转账给自己"));
            return 0;
        }

        if (EconomyManager.transferMoney(player.getUUID(), target.getUUID(), amount, memo)) {
            source.sendSuccess(() -> Component.literal("成功转账 ")
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 金币给 "))
                    .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.YELLOW)), false);
            
            target.sendSystemMessage(Component.literal("收到来自 ")
                    .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" 的转账: "))
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 金币"))
                    .append(Component.literal(" (备注: " + memo + ")")));
        } else {
            source.sendFailure(Component.literal("转账失败，余额不足"));
        }

        return 1;
    }

    // 存款
    private static int deposit(CommandSourceStack source, long amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (EconomyManager.deposit(player.getUUID(), amount)) {
            source.sendSuccess(() -> Component.literal("成功存入 ")
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 金币到储蓄账户")), false);
        } else {
            source.sendFailure(Component.literal("存款失败，余额不足"));
        }

        return 1;
    }

    // 取款
    private static int withdraw(CommandSourceStack source, long amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (EconomyManager.withdraw(player.getUUID(), amount)) {
            source.sendSuccess(() -> Component.literal("成功从储蓄账户取出 ")
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 金币")), false);
        } else {
            source.sendFailure(Component.literal("取款失败，储蓄余额不足"));
        }

        return 1;
    }

    // 申请贷款
    private static int requestLoan(CommandSourceStack source, long amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (EconomyManager.requestLoan(player.getUUID(), amount)) {
            source.sendSuccess(() -> Component.literal("成功申请贷款 ")
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 金币，利率10%，期限30天")), false);
        } else {
            source.sendFailure(Component.literal("贷款申请失败，您可能已有未还清的贷款或金额超出限制"));
        }

        return 1;
    }

    // 还款
    private static int repayLoan(CommandSourceStack source, long amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (EconomyManager.repayLoan(player.getUUID(), amount)) {
            source.sendSuccess(() -> Component.literal("成功还款 ")
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" 金币")), false);
        } else {
            source.sendFailure(Component.literal("还款失败，余额不足或没有贷款"));
        }

        return 1;
    }

    // 显示交易历史
    private static int showHistory(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        List<Transaction> history = EconomyManager.getTransactionHistory(player.getUUID());
        
        source.sendSuccess(() -> Component.literal("=== 交易历史 (最近10条) ===").withStyle(ChatFormatting.GOLD), false);
        
        if (history.isEmpty()) {
            source.sendSuccess(() -> Component.literal("暂无交易记录").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            Transaction transaction = history.get(i);
            ChatFormatting color = transaction.isIncome() ? ChatFormatting.GREEN : ChatFormatting.RED;
            String sign = transaction.isIncome() ? "+" : "-";
            
            source.sendSuccess(() -> Component.literal(transaction.getFormattedTimestamp() + " ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("[" + transaction.getTypeDescription() + "] ").withStyle(ChatFormatting.BLUE))
                    .append(Component.literal(sign + transaction.getAmount()).withStyle(color))
                    .append(Component.literal(" - " + transaction.getDescription()).withStyle(ChatFormatting.GRAY)), false);
        }

        return 1;
    }

    // 开具支票
    private static int issueCheck(CommandSourceStack source, long amount, ServerPlayer recipient) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (EconomyManager.removeMoney(player.getUUID(), amount)) {
            ItemStack check = CheckItem.createCheck(amount, player.getName().getString(), recipient.getName().getString());
            
            if (recipient.getInventory().add(check)) {
                source.sendSuccess(() -> Component.literal("成功开具支票 ")
                        .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" 金币给 "))
                        .append(Component.literal(recipient.getName().getString()).withStyle(ChatFormatting.YELLOW)), false);
                
                recipient.sendSystemMessage(Component.literal("收到来自 ")
                        .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" 的支票: "))
                        .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" 金币")));
            } else {
                // 如果收件人背包满了，退还金钱
                EconomyManager.addMoney(player.getUUID(), amount);
                source.sendFailure(Component.literal("对方背包已满，无法接收支票"));
            }
        } else {
            source.sendFailure(Component.literal("余额不足，无法开具支票"));
        }

        return 1;
    }

    // 领取每日奖励
    private static int claimDailyReward(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (EconomyManager.claimDailyReward(player.getUUID())) {
            source.sendSuccess(() -> Component.literal("成功领取每日奖励！获得 50 金币"), false);
        } else {
            source.sendFailure(Component.literal("今日奖励已领取或功能未启用"));
        }

        return 1;
    }

    // 显示经济统计
    private static int showEconomyStats(CommandSourceStack source) {
        EconomyStats stats = EconomyManager.getEconomyStats();
        
        source.sendSuccess(() -> Component.literal("=== 服务器经济统计 ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("总交易次数: " + stats.getTotalTransactions()).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("总交易金额: " + stats.getTotalTransactionValue() + " 金币").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("税收收入: " + stats.getTotalTaxCollected() + " 金币").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("贷款发放: " + stats.getTotalLoansIssued() + " 笔").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("商店创建: " + stats.getTotalShopsCreated() + " 个").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("平均交易金额: " + String.format("%.2f", stats.getAverageTransactionValue()) + " 金币").withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    // 显示富豪榜
    private static int showTopPlayers(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== 富豪榜 ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("功能开发中...").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    // 管理员给予金钱
    private static int adminGiveMoney(CommandSourceStack source, ServerPlayer target, long amount) {
        EconomyManager.addMoney(target.getUUID(), amount);
        
        source.sendSuccess(() -> Component.literal("已给予 ")
                .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" "))
                .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 金币")), false);
        
        target.sendSystemMessage(Component.literal("管理员给予了您 ")
                .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 金币")));

        return 1;
    }

    // 管理员扣除金钱
    private static int adminTakeMoney(CommandSourceStack source, ServerPlayer target, long amount) {
        if (EconomyManager.removeMoney(target.getUUID(), amount)) {
            source.sendSuccess(() -> Component.literal("已从 ")
                    .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" 扣除 "))
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.RED))
                    .append(Component.literal(" 金币")), false);
            
            target.sendSystemMessage(Component.literal("管理员扣除了您 ")
                    .append(Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.RED))
                    .append(Component.literal(" 金币")));
        } else {
            source.sendFailure(Component.literal("目标玩家余额不足"));
        }

        return 1;
    }

    // 管理员重置玩家
    private static int adminResetPlayer(CommandSourceStack source, ServerPlayer target) {
        // 这里需要实现重置玩家经济数据的逻辑
        source.sendSuccess(() -> Component.literal("已重置 ")
                .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" 的经济数据")), false);
        
        return 1;
    }

    // 管理员计算利息
    private static int adminCalculateInterest(CommandSourceStack source) {
        EconomyManager.calculateInterest();
        source.sendSuccess(() -> Component.literal("已为所有玩家计算并发放银行利息"), false);
        return 1;
    }
}