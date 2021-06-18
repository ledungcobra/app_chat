package server.handler;

import common.dto.*;
import server.entities.Group;
import server.entities.User;
import utils.SocketExtension;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static common.dto.Command.*;
import static server.context.SApplicationContext.userService;

public class GroupRequestHandler extends RequestHandler {
    public GroupRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command) {

        switch (command) {
            case C2S_FIND_GROUP_BY_KEYWORD:
                return Optional.of(this::findGroupByKeyword);
            case C2S_SEND_JOIN_GROUPS:
                return Optional.of(this::joinGroups);
            case C2S_LEAVE_GROUP:
                return Optional.of(this::leaveGroup);
            case C2S_ADD_MEMBER_TO_GROUP:
                return Optional.of(this::addMemberToGroup);
            case C2S_GET_ALL_MEMBERS_IN_GROUP:
                return Optional.of(this::getAllMembersInGroup);
            case C2S_CREATE_NEW_GROUP:
                return Optional.of(this::createNewGroup);
            case C2S_ACCEPT_A_MEMBER_TO_GROUP:
                return Optional.of(this::acceptAMember);
            case C2S_ADD_ADMIN:
                return Optional.of(this::addAdmin);
            case C2S_REMOVE_ADMIN:
                return Optional.of(this::removeAdmin);
            case C2S_REMOVE_MEMBER:
                return Optional.of(this::removeMember);
            case C2S_DECLINE_USER:
                return Optional.of(this::declineUser);
        }
        return Optional.empty();
    }

    /**
     * payload is pendingId
     *
     * @param commandObject
     */
    private void declineUser(CommandObject commandObject) {
        try {
            Long pendingId = (Long) commandObject.getPayload();

            if (pendingId == null) {
                sendResponseAsync(new CommandObject(S2C_DECLINE_USER_NACK, "PendingId  cannot be null"));
                return;
            }

             userService.removePending(pendingId);
            sendResponseAsync(new CommandObject(S2C_DECLINE_USER_ACK, pendingId));

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_DECLINE_USER_NACK, e.getMessage()));
        }
    }

    /**
     * Payload is [userId, groupId]
     *
     * @param commandObject return userID
     */

    private void removeMember(CommandObject commandObject) {
        try {
            Long[] ids = (Long[]) commandObject.getPayload();
            if (ids == null || ids.length < 2) {
                sendResponseAsync(new CommandObject(S2C_REMOVE_MEMBER_NACK, "Payload is invalid"));
                return;
            }
            Long userId = ids[0];
            Long groupId = ids[1];
            userService.leaveGroup(groupId, userId);
            sendResponseAsync(new CommandObject(S2C_REMOVE_MEMBER_ACK, userId));
        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_REMOVE_MEMBER_NACK, e.getMessage()));
        }
    }

    private boolean isAdmin(Long userId, Long groupId) {
        return userService.isAdmin(userId, groupId);
    }

    /**
     * payload is [userId, groupId]
     *
     * @param commandObject
     */
    private void removeAdmin(CommandObject commandObject) {

        try {

            Long[] ids = (Long[]) commandObject.getPayload();
            if (ids == null || ids.length < 2) {
                sendResponseAsync(new CommandObject(S2C_ADD_ADMIN_NACK, "Payload invalid"));
                return;
            }

            Long userId = ids[0];
            Long groupId = ids[1];
            userService.removeAdmin(userId, groupId);
            sendResponseAsync(new CommandObject(S2C_REMOVE_ADMIN_ACK));

        } catch (Exception e) {

        }

    }

    /**
     * payload is [userId, groupId]
     *
     * @param commandObject
     */

    private void addAdmin(CommandObject commandObject) {
        try {

            Long[] ids = (Long[]) commandObject.getPayload();
            if (ids == null || ids.length < 2) {
                sendResponseAsync(new CommandObject(S2C_ADD_ADMIN_NACK, "Payload invalid"));
                return;
            }
            Long userId = ids[0];
            Long groupId = ids[1];
            userService.addAdmin(userId, groupId);
            sendResponseAsync(new CommandObject(S2C_ADD_ADMIN_ACK, ids));

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_ADD_ADMIN_NACK, e.getMessage()));
        }
    }

    /**
     * payload is pendingId
     *
     * @param commandObject note : another should update group list if accept success fully
     */
    private void acceptAMember(CommandObject commandObject) {
        try {
            Long pendingId = (Long) commandObject.getPayload();
            if (pendingId == null) {
                sendResponseAsync(new CommandObject(S2C_ACCEPT_A_MEMBER_TO_GROUP_NACK, "PendingId  cannot be null"));
                return;
            }
            Object[] result = userService.acceptAMember(pendingId);

            if (result == null) {
                sendResponseAsync(new CommandObject(S2C_ACCEPT_A_MEMBER_TO_GROUP_NACK, "An error occur"));
                return;
            }

            sendResponseAsync(new CommandObject(S2C_ACCEPT_A_MEMBER_TO_GROUP_ACK, Mapper2.map(result[0], UserDto.class)));

            getFriendSocket((User) result[0]).ifPresent(s -> {
                SocketExtension.sendResponseToSocket(s,
                        new CommandObject(S2C_NEW_ACCEPTED_GROUP, Mapper.<Group, GroupDto>map((Group) result[1])));

            });

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_ACCEPT_A_MEMBER_TO_GROUP_NACK, e.getMessage()));
        }
    }

    /**
     * payload is group dto
     *
     * @param commandObject
     */
    private void createNewGroup(CommandObject commandObject) {
        try {
            GroupDto groupDto = (GroupDto) commandObject.getPayload();
            if (groupDto == null) {
                sendResponseAsync(new CommandObject(S2C_CREATE_NEW_GROUP_NACK, "GroupDto  cannot be null"));
                return;
            }
            Group newGroup = userService.createNewGroup(groupDto.getName(), getCurrentUser().getId());

            sendResponseAsync(new CommandObject(S2C_CREATE_NEW_GROUP_ACK, Mapper2.map(newGroup, GroupDto.class)));
        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_CREATE_NEW_GROUP_NACK, e.getMessage()));
        }
    }

    /**
     * payload is groupId (Long)
     * send group id if success
     *
     * @param commandObject
     */
    private void leaveGroup(CommandObject commandObject) {
        try {
            Long groupId = (Long) commandObject.getPayload();
            if (groupId == null) {
                sendResponseAsync(new CommandObject(S2C_LEAVE_GROUP_NACK, "Group Id cannot be null"));
                return;
            }
            userService.leaveGroup(groupId, getCurrentUser().getId());
            sendResponseAsync(new CommandObject(S2C_LEAVE_GROUP_ACK, groupId));
        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_LEAVE_GROUP_NACK, e.getMessage()));
        }
    }

    /**
     * Take Long[] array contains ids of group want to join
     *
     * @param commandObject
     */
    private void joinGroups(CommandObject commandObject) {
        Long[] ids = (Long[]) commandObject.getPayload();
        User user = getCurrentUser();

        for (Long id : ids) {
            try {
                userService.joinGroup(user.getId(), id);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponseAsync(new CommandObject(S2C_SEND_JOIN_GROUPS_NACK, "You already send this request to group id = " + id));
            }
        }
        sendResponseAsync(new CommandObject(S2C_SEND_JOIN_GROUPS_ACK, "Waiting for admin accept"));
    }

    /**
     * input is string : keyword
     * output is CommandObject: contains payload either message or list<GroupDto>
     *
     * @param commandObject
     */
    private void findGroupByKeyword(CommandObject commandObject) {

        try {
            String keyword = (String) commandObject.getPayload();
            if (keyword == null || keyword.isEmpty()) {
                sendResponseAsync(new CommandObject(S2C_FIND_GROUP_BY_KEYWORD_NACK, "Keyword is empty"));
                return;
            }
            List<Group> groups = userService.findGroupByKeyword(keyword);
            sendResponseAsync(new CommandObject(S2C_SEND_JOIN_GROUPS_ACK,
                    groups.stream().map(Mapper::<Group, GroupDto>map).collect(Collectors.toList())));

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_FIND_GROUP_BY_KEYWORD_NACK, e.getMessage()));
        }
    }

    /**
     * payload is userId, groupId
     * send group userId, groupId
     *
     * @param commandObject
     */
    private void addMemberToGroup(CommandObject commandObject) {
        try {
            Long[] ids = (Long[]) commandObject.getPayload();

            if (ids.length < 2) {
                sendResponseAsync(new CommandObject(S2C_ADD_MEMBER_TO_GROUP_NACK, "Invalid payload"));
                return;
            }
            userService.addMemberToGroup(ids[0], ids[1]);
            sendResponseAsync(new CommandObject(S2C_ADD_MEMBER_TO_GROUP_ACK, ids));

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_FIND_GROUP_BY_KEYWORD_NACK, e.getMessage()));
        }
    }

    /**
     * payload is group id
     *
     * @param commandObject
     */
    private void getAllMembersInGroup(CommandObject commandObject) {
        try {
            Long groupId = (Long) commandObject.getPayload();
            if (groupId == null) {
                sendResponseAsync(new CommandObject(S2C_GET_ALL_MEMBERS_IN_GROUP_NACK, "Group id cannot be null"));
                return;
            }

            List<User> members = userService.getAllMembers(groupId);
            List<UserDto> membersDtos = members.stream().map(m -> Mapper.<User, UserDto>map(m)).collect(Collectors.toList());
            sendResponseAsync(new CommandObject(S2C_GET_ALL_MEMBERS_IN_GROUP_ACK, membersDtos));

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_GET_ALL_MEMBERS_IN_GROUP_NACK, e.getMessage()));
        }
    }


}
